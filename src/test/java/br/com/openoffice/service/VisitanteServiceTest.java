package br.com.openoffice.service;

import br.com.openoffice.BaseIntegracaoTest;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.LimiteVisitantesException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.visita.Visita;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisitanteServiceTest extends BaseIntegracaoTest {

    @Autowired
    private VisitanteService visitantes;

    @Test
    @DisplayName("Cada membro pode ter no máximo 3 visitantes ao mesmo tempo")
    void limiteDeTres() {
        Membro m = novoMembro(TipoPlano.MENSAL);
        visitantes.registrarEntrada(m, "Visitante A", "111.111.111-" + m.getId());
        visitantes.registrarEntrada(m, "Visitante B", "222.222.222-" + m.getId());
        Visita c = visitantes.registrarEntrada(m, "Visitante C", "333.333.333-" + m.getId());

        assertThatThrownBy(() -> visitantes.registrarEntrada(m, "Visitante D", "444.444.444-" + m.getId()))
                .isInstanceOf(LimiteVisitantesException.class);

        visitantes.registrarSaida(m, c.getId());
        assertThat(visitantes.registrarEntrada(m, "Visitante D", "444.444.444-" + m.getId())).isNotNull();
        assertThat(visitantes.historicoDoMembro(m.getId())).hasSize(4);
    }

    @Test
    @DisplayName("Só membro com plano ativo pode autorizar visitantes")
    void exigePlanoAtivo() {
        Membro semPlano = novoMembro(null);
        assertThatThrownBy(() -> visitantes.registrarEntrada(semPlano, "Fulano", "12345678"))
                .isInstanceOf(PlanoInativoException.class);
    }

    @Test
    @DisplayName("O mesmo visitante não pode entrar duas vezes sem registrar a saída")
    void naoEntraDuasVezes() {
        Membro a = novoMembro(TipoPlano.MENSAL);
        Membro b = novoMembro(TipoPlano.MENSAL);
        String doc = "RG-" + a.getId();
        visitantes.registrarEntrada(a, "Maria", doc);
        assertThatThrownBy(() -> visitantes.registrarEntrada(b, "Maria", doc))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("já está no espaço");
    }
}
