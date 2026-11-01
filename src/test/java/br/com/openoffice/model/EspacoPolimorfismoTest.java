package br.com.openoffice.model;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.plano.Assinatura;
import br.com.openoffice.regra.PoliticaCancelamento;
import br.com.openoffice.util.Validador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes de unidade das classes de domínio (sem Spring). */
class EspacoPolimorfismoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 2);

    private Membro membroCom(TipoPlano plano) {
        Membro m = new Membro("Teste", "526.018.159-06", "t@t.com", "", "hash", HOJE.atStartOfDay());
        if (plano != null) {
            m.adicionarAssinatura(new Assinatura("U1", plano, HOJE, HOJE.atStartOfDay()));
        }
        return m;
    }

    @Test
    @DisplayName("Cada subclasse de Espaco define o seu preço por hora (polimorfismo)")
    void precosPorTipo() {
        List<Espaco> espacos = List.of(
                TipoEspaco.ESTACAO.criar("E", 1, ""),
                TipoEspaco.SALA_REUNIAO.criar("R", 8, ""),
                TipoEspaco.SALA_PRIVADA.criar("P", 4, ""),
                TipoEspaco.AUDITORIO.criar("A", 100, ""));
        List<String> esperado = List.of("0.00", "60.00", "80.00", "200.00");
        for (int i = 0; i < espacos.size(); i++) {
            assertThat(espacos.get(i).getPrecoHora()).isEqualByComparingTo(esperado.get(i));
        }
    }

    @Test
    @DisplayName("Desconto VIP de 20% só em sala de reunião e auditório")
    void descontoPorTipo() {
        Duration duasHoras = Duration.ofHours(2);
        assertThat(TipoEspaco.SALA_REUNIAO.criar("R", 8, "").calcularValor(duasHoras, true).total())
                .isEqualByComparingTo("96.00");
        assertThat(TipoEspaco.AUDITORIO.criar("A", 100, "").calcularValor(duasHoras, true).total())
                .isEqualByComparingTo("320.00");
        assertThat(TipoEspaco.SALA_PRIVADA.criar("P", 4, "").calcularValor(duasHoras, true).total())
                .isEqualByComparingTo("160.00");
        assertThat(TipoEspaco.SALA_REUNIAO.criar("R", 8, "").calcularValor(Duration.ofMinutes(90), false).total())
                .isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("SalaPrivada sobrescreve validarAcesso para exigir VIP")
    void acessoSalaPrivada() {
        Espaco privada = TipoEspaco.SALA_PRIVADA.criar("P", 4, "");
        assertThatThrownBy(() -> privada.validarAcesso(membroCom(null), HOJE)).isInstanceOf(PlanoInativoException.class);
        assertThatThrownBy(() -> privada.validarAcesso(membroCom(TipoPlano.MENSAL), HOJE))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatCode(() -> privada.validarAcesso(membroCom(TipoPlano.VIP), HOJE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Estação de trabalho sempre tem capacidade 1")
    void estacaoCapacidadeFixa() {
        Espaco estacao = TipoEspaco.ESTACAO.criar("E", 1, "");
        estacao.setCapacidade(10);
        assertThat(estacao.getCapacidade()).isEqualTo(1);
    }

    @Test
    @DisplayName("Plano vence sozinho: ativo só dentro do período")
    void vencimentoDoPlano() {
        Membro m = membroCom(TipoPlano.SEMANAL);
        assertThat(m.temPlanoAtivo(HOJE.plusDays(6))).isTrue();
        assertThat(m.temPlanoAtivo(HOJE.plusDays(7))).isFalse();
        assertThat(m.getDiasAntecedenciaMaxima(HOJE)).isEqualTo(7);
        assertThat(membroCom(TipoPlano.VIP).getDiasAntecedenciaMaxima(HOJE)).isEqualTo(30);
    }

    @Test
    @DisplayName("Política de cancelamento: multa de 50% com menos de 24h")
    void politicaCancelamento() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        assertThat(PoliticaCancelamento.calcularMulta(new java.math.BigDecimal("320.00"), inicio, inicio.minusHours(23)))
                .isEqualByComparingTo("160.00");
        assertThat(PoliticaCancelamento.calcularMulta(new java.math.BigDecimal("320.00"), inicio, inicio.minusHours(24)))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Validações de CPF, e-mail e cartão (Luhn)")
    void validador() {
        assertThat(Validador.cpfValido("526.018.159-06")).isTrue();
        assertThat(Validador.cpfValido("526.018.159-07")).isFalse();
        assertThat(Validador.cpfValido("111.111.111-11")).isFalse();
        assertThat(Validador.emailValido("ana@email.com")).isTrue();
        assertThat(Validador.emailValido("ana@")).isFalse();
        assertThat(Validador.cartaoValido("4242 4242 4242 4242")).isTrue();
        assertThat(Validador.cartaoValido("4242 4242 4242 4241")).isFalse();
    }
}
