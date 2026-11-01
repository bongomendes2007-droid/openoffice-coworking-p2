package br.com.openoffice.service;

import br.com.openoffice.BaseIntegracaoTest;
import br.com.openoffice.enums.StatusInscricao;
import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.CapacidadeExcedidaException;
import br.com.openoffice.exception.EventoLotadoException;
import br.com.openoffice.exception.HorarioIndisponivelException;
import br.com.openoffice.exception.InscricaoNaoPermitidaException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.model.evento.Inscricao;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.dto.NovoEvento;
import br.com.openoffice.service.dto.SolicitacaoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoServiceTest extends BaseIntegracaoTest {

    @Autowired
    private EventoService eventos;
    @Autowired
    private ReservaService reservas;
    @Autowired
    private FinanceiroService financeiro;

    private Evento criar(Espaco espaco, int diasAFrente, int vagas, String preco, boolean especial, int diasVip) {
        LocalDateTime inicio = AGORA.toLocalDate().plusDays(diasAFrente).atTime(19, 0);
        return eventos.criar(new NovoEvento("Evento " + espaco.getId(), "teste", espaco.getId(), inicio,
                inicio.plusHours(2), vagas, new BigDecimal(preco), especial, diasVip));
    }

    @Test
    @DisplayName("Inscrições abrem primeiro para VIP")
    void preVendaVip() {
        Evento e = criar(novoEspaco(TipoEspaco.AUDITORIO, 100), 10, 50, "100.00", false, 3);
        Membro mensal = novoMembro(TipoPlano.MENSAL);
        assertThatThrownBy(() -> eventos.inscrever(mensal, e.getId(), DadosPagamento.pix()))
                .isInstanceOf(InscricaoNaoPermitidaException.class)
                .hasMessageContaining("só para membros VIP");
        Membro vip = novoMembro(TipoPlano.VIP);
        assertThat(eventos.inscrever(vip, e.getId(), DadosPagamento.pix()).getStatus())
                .isEqualTo(StatusInscricao.CONFIRMADA);
    }

    @Test
    @DisplayName("VIP tem 15% de desconto apenas em eventos especiais")
    void descontoEventoEspecial() {
        Espaco auditorio = novoEspaco(TipoEspaco.AUDITORIO, 100);
        Evento especial = criar(auditorio, 5, 50, "150.00", true, 0);
        Evento comum = criar(auditorio, 6, 50, "150.00", false, 0);
        Membro vip = novoMembro(TipoPlano.VIP);
        Membro mensal = novoMembro(TipoPlano.MENSAL);

        assertThat(eventos.inscrever(vip, especial.getId(), DadosPagamento.pix()).getValor()).isEqualByComparingTo("127.50");
        assertThat(eventos.inscrever(vip, comum.getId(), DadosPagamento.pix()).getValor()).isEqualByComparingTo("150.00");
        assertThat(eventos.inscrever(mensal, especial.getId(), DadosPagamento.pix()).getValor()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Evento lotado: lista de espera e promoção automática quando alguém cancela")
    void listaDeEspera() {
        Evento e = criar(novoEspaco(TipoEspaco.SALA_REUNIAO, 8), 5, 1, "40.00", false, 0);
        Membro primeiro = novoMembro(TipoPlano.MENSAL);
        Membro segundo = novoMembro(TipoPlano.MENSAL);
        eventos.inscrever(primeiro, e.getId(), DadosPagamento.pix());

        assertThatThrownBy(() -> eventos.inscrever(segundo, e.getId(), DadosPagamento.pix()))
                .isInstanceOf(EventoLotadoException.class);
        assertThat(eventos.entrarNaListaEspera(segundo, e.getId())).isEqualTo(1);

        eventos.cancelarInscricao(primeiro, e.getId());

        Inscricao promovida = eventos.buscar(e.getId()).inscricaoAtivaDe(segundo.getId()).orElseThrow();
        assertThat(promovida.getStatus()).isEqualTo(StatusInscricao.AGUARDANDO_PAGAMENTO);
        Cobranca pendente = financeiro.pendentes(segundo.getId()).get(0);

        financeiro.pagar(segundo, pendente.getId(), DadosPagamento.pix());
        eventos.confirmarPagamentoInscricao(financeiro.buscar(pendente.getId()));
        assertThat(eventos.buscar(e.getId()).inscricaoAtivaDe(segundo.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusInscricao.CONFIRMADA);
    }

    @Test
    @DisplayName("Evento respeita a capacidade do espaço e não conflita com reservas")
    void capacidadeEConflito() {
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        assertThatThrownBy(() -> criar(sala, 5, 20, "0", false, 0))
                .isInstanceOf(CapacidadeExcedidaException.class);

        Membro m = novoMembro(TipoPlano.MENSAL);
        reservas.reservar(m, new SolicitacaoReserva(sala.getId(), AGORA.toLocalDate().plusDays(3),
                LocalTime.of(19, 0), LocalTime.of(20, 0), 2), DadosPagamento.pix());
        assertThatThrownBy(() -> criar(sala, 3, 5, "0", false, 0))
                .isInstanceOf(HorarioIndisponivelException.class);
    }
}
