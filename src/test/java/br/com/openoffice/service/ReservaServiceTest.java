package br.com.openoffice.service;

import br.com.openoffice.BaseIntegracaoTest;
import br.com.openoffice.enums.StatusCobranca;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.AntecedenciaInvalidaException;
import br.com.openoffice.exception.CapacidadeExcedidaException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.HorarioIndisponivelException;
import br.com.openoffice.exception.PagamentoRecusadoException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.service.dto.ResultadoCancelamento;
import br.com.openoffice.service.dto.SolicitacaoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaServiceTest extends BaseIntegracaoTest {

    private static final LocalDate AMANHA = AGORA.toLocalDate().plusDays(1);

    @Autowired
    private ReservaService reservas;
    @Autowired
    private FinanceiroService financeiro;

    private SolicitacaoReserva pedido(Espaco e, LocalDate dia, int inicio, int fim, int pessoas) {
        return new SolicitacaoReserva(e.getId(), dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0), pessoas);
    }

    @Test
    @DisplayName("VIP recebe 20% de desconto na sala de reunião (2h x R$ 60 = R$ 120 → R$ 96)")
    void descontoVipSalaReuniao() {
        Membro vip = novoMembro(TipoPlano.VIP);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        Reserva r = reservas.reservar(vip, pedido(sala, AMANHA, 14, 16, 4), DadosPagamento.pix());
        assertThat(r.getValorBase()).isEqualByComparingTo("120.00");
        assertThat(r.getDesconto()).isEqualByComparingTo("24.00");
        assertThat(r.getValorTotal()).isEqualByComparingTo("96.00");
    }

    @Test
    @DisplayName("Membro sem VIP paga o preço cheio do auditório (R$ 200/h)")
    void auditorioSemDesconto() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco auditorio = novoEspaco(TipoEspaco.AUDITORIO, 100);
        Reserva r = reservas.reservar(m, pedido(auditorio, AMANHA, 9, 11, 50), DadosPagamento.pix());
        assertThat(r.getValorTotal()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Não permite reservas com horários sobrepostos no mesmo espaço")
    void semSobreposicao() {
        Membro a = novoMembro(TipoPlano.MENSAL);
        Membro b = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        reservas.reservar(a, pedido(sala, AMANHA, 14, 16, 2), DadosPagamento.pix());

        assertThatThrownBy(() -> reservas.reservar(b, pedido(sala, AMANHA, 15, 17, 2), DadosPagamento.pix()))
                .isInstanceOf(HorarioIndisponivelException.class)
                .hasMessageContaining("das 14:00 às 16:00")
                .hasMessageContaining("16:00–18:00");

        // Horário encostado (começa quando o outro termina) é permitido.
        Reserva seguinte = reservas.reservar(b, pedido(sala, AMANHA, 16, 17, 2), DadosPagamento.pix());
        assertThat(seguinte.getId()).isNotNull();
    }

    @Test
    @DisplayName("Número de pessoas não pode passar da capacidade")
    void capacidade() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        assertThatThrownBy(() -> reservas.simular(m, pedido(sala, AMANHA, 9, 10, 9)))
                .isInstanceOf(CapacidadeExcedidaException.class);
    }

    @Test
    @DisplayName("Sala privada é exclusiva para VIP")
    void salaPrivadaSoVip() {
        Espaco privada = novoEspaco(TipoEspaco.SALA_PRIVADA, 4);
        Membro mensal = novoMembro(TipoPlano.MENSAL);
        assertThatThrownBy(() -> reservas.simular(mensal, pedido(privada, AMANHA, 9, 10, 2)))
                .isInstanceOf(AcessoNegadoException.class);

        Membro vip = novoMembro(TipoPlano.VIP);
        assertThat(reservas.simular(vip, pedido(privada, AMANHA, 9, 10, 2)).total()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("Antecedência: até 7 dias para planos comuns e até 30 dias para VIP")
    void antecedencia() {
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        Membro mensal = novoMembro(TipoPlano.MENSAL);
        LocalDate hoje = AGORA.toLocalDate();

        assertThat(reservas.simular(mensal, pedido(sala, hoje.plusDays(7), 9, 10, 2))).isNotNull();
        assertThatThrownBy(() -> reservas.simular(mensal, pedido(sala, hoje.plusDays(8), 9, 10, 2)))
                .isInstanceOf(AntecedenciaInvalidaException.class)
                .hasMessageContaining("7 dias");

        Membro vip = novoMembro(TipoPlano.VIP);
        assertThat(reservas.simular(vip, pedido(sala, hoje.plusDays(25), 9, 10, 2))).isNotNull();
    }

    @Test
    @DisplayName("Sem plano ativo não é possível reservar")
    void semPlano() {
        Membro semPlano = novoMembro(null);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        assertThatThrownBy(() -> reservas.simular(semPlano, pedido(sala, AMANHA, 9, 10, 2)))
                .isInstanceOf(PlanoInativoException.class);
    }

    @Test
    @DisplayName("Horários inválidos: passado, fora do funcionamento, fim antes do início, fora dos blocos de 30 min")
    void horariosInvalidos() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        LocalDate hoje = AGORA.toLocalDate();
        assertThatThrownBy(() -> reservas.simular(m, pedido(sala, hoje, 8, 9, 2)))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("já passou");
        assertThatThrownBy(() -> reservas.simular(m, pedido(sala, AMANHA, 21, 23, 2)))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("22:00");
        assertThatThrownBy(() -> reservas.simular(m, pedido(sala, AMANHA, 15, 14, 2)))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("depois do início");
        assertThatThrownBy(() -> reservas.simular(m, new SolicitacaoReserva(sala.getId(), AMANHA,
                LocalTime.of(9, 15), LocalTime.of(10, 0), 2)))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("30 minutos");
    }

    @Test
    @DisplayName("Cancelar com menos de 24h retém 50% do valor como multa")
    void cancelamentoComMulta() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        // Agora: segunda 10:00. Reserva: terça 09:00 (faltam 23h).
        Reserva r = reservas.reservar(m, pedido(sala, AMANHA, 9, 11, 2), DadosPagamento.pix());

        ResultadoCancelamento resultado = reservas.cancelar(m, r.getId());

        assertThat(resultado.cobraMulta()).isTrue();
        assertThat(resultado.multa()).isEqualByComparingTo("60.00");
        assertThat(resultado.reembolso()).isEqualByComparingTo("60.00");
        List<Cobranca> historico = financeiro.historico(m.getId());
        assertThat(historico).anyMatch(c -> c.getTipo() == TipoCobranca.MULTA
                && c.getValor().compareTo(new BigDecimal("60.00")) == 0);
        assertThat(financeiro.buscar(r.getCobrancaId()).getStatus()).isEqualTo(StatusCobranca.ESTORNADA);
    }

    @Test
    @DisplayName("Cancelar com 24h ou mais de antecedência não tem multa")
    void cancelamentoSemMulta() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        Reserva r = reservas.reservar(m, pedido(sala, AMANHA, 10, 11, 2), DadosPagamento.pix());
        ResultadoCancelamento resultado = reservas.cancelar(m, r.getId());
        assertThat(resultado.multa()).isEqualByComparingTo("0");
        assertThat(resultado.reembolso()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Cartão recusado não grava a reserva nem a cobrança")
    void cartaoRecusado() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        Espaco sala = novoEspaco(TipoEspaco.SALA_REUNIAO, 8);
        int antes = financeiro.historico(m.getId()).size();
        assertThatThrownBy(() -> reservas.reservar(m, pedido(sala, AMANHA, 9, 10, 2), cartao("1234 5678 9012 3456")))
                .isInstanceOf(PagamentoRecusadoException.class);
        assertThat(reservas.doMembro(m.getId())).isEmpty();
        assertThat(financeiro.historico(m.getId())).hasSize(antes);

        Reserva ok = reservas.reservar(m, pedido(sala, AMANHA, 9, 10, 2), cartao("4242 4242 4242 4242"));
        assertThat(ok.getCobrancaId()).isNotNull();
    }

    @Test
    @DisplayName("Estação de trabalho está incluída no plano (sem cobrança)")
    void estacaoGratuita() {
        Membro m = novoMembro(TipoPlano.DIARIO);
        Espaco estacao = novoEspaco(TipoEspaco.ESTACAO, 1);
        Reserva r = reservas.reservar(m, pedido(estacao, AGORA.toLocalDate(), 13, 18, 1), null);
        assertThat(r.getValorTotal()).isEqualByComparingTo("0");
        assertThat(r.getCobrancaId()).isNull();
    }
}
