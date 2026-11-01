package br.com.openoffice.service;

import br.com.openoffice.BaseIntegracaoTest;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.AutenticacaoException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.plano.Assinatura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanoEAutenticacaoTest extends BaseIntegracaoTest {

    @Autowired
    private AutenticacaoService autenticacao;
    @Autowired
    private FinanceiroService financeiro;

    @Test
    @DisplayName("Preços dos planos conforme o enunciado")
    void precos() {
        assertThat(TipoPlano.DIARIO.getPreco()).isEqualByComparingTo("50.00");
        assertThat(TipoPlano.SEMANAL.getPreco()).isEqualByComparingTo("200.00");
        assertThat(TipoPlano.MENSAL.getPreco()).isEqualByComparingTo("600.00");
        assertThat(TipoPlano.VIP.getPreco()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("Renovação começa no dia seguinte ao fim do plano atual")
    void renovacao() {
        Membro m = novoMembro(TipoPlano.SEMANAL);
        Assinatura nova = planoService.assinar(m, TipoPlano.VIP, DadosPagamento.pix());
        assertThat(nova.getInicio()).isEqualTo(AGORA.toLocalDate().plusDays(7));
        assertThat(m.isVip(AGORA.toLocalDate())).isFalse();
        assertThat(m.isVip(AGORA.toLocalDate().plusDays(7))).isTrue();
        assertThat(financeiro.totalPago(m.getId())).isEqualByComparingTo("1100.00");
    }

    @Test
    @DisplayName("Senha é guardada como hash e o login confere a senha")
    void loginComHash() {
        Membro m = novoMembro(null);
        assertThat(m.getSenhaHash()).doesNotContain("senha123").startsWith("$2a$");
        assertThat(autenticacao.autenticar(m.getEmail(), "senha123").getId()).isEqualTo(m.getId());
    }

    @Test
    @DisplayName("Depois de 5 tentativas erradas o login é bloqueado")
    void bloqueioPorTentativas() {
        Membro m = novoMembro(null);
        for (int i = 1; i <= 4; i++) {
            int restantes = 5 - i;
            assertThatThrownBy(() -> autenticacao.autenticar(m.getEmail(), "errada"))
                    .isInstanceOf(AutenticacaoException.class)
                    .hasMessageContaining("Tentativas restantes: " + restantes);
        }
        assertThatThrownBy(() -> autenticacao.autenticar(m.getEmail(), "errada"))
                .hasMessageContaining("bloqueado");
        assertThatThrownBy(() -> autenticacao.autenticar(m.getEmail(), "senha123"))
                .hasMessageContaining("Tente novamente");
    }

    @Test
    @DisplayName("Cadastro recusa CPF inválido, CPF repetido e e-mail repetido")
    void cadastroDuplicado() {
        Membro m = novoMembro(null);
        assertThatThrownBy(() -> membroService.cadastrar("X", "111.111.111-11", "x@x.com", "", "123456"))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("CPF inválido");
        assertThatThrownBy(() -> membroService.cadastrar("X", m.getDocumento(), "novo@x.com", "", "123456"))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("CPF");
        assertThatThrownBy(() -> membroService.cadastrar("X", gerarCpf(99999), m.getEmail(), "", "123456"))
                .isInstanceOf(DadosInvalidosException.class).hasMessageContaining("e-mail");
    }
}
