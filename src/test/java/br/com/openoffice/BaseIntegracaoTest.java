package br.com.openoffice;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.EspacoService;
import br.com.openoffice.service.MembroService;
import br.com.openoffice.service.PlanoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base dos testes de serviço: sobe o sistema com uma pasta de dados temporária,
 * sem dados de demonstração e com o relógio parado em segunda-feira, 02/11/2026 às 10:00.
 */
@SpringBootTest(properties = {
        "openoffice.data-dir=target/dados-teste/${random.uuid}",
        "openoffice.dados-demonstracao=false",
        "openoffice.relogio-fixo=2026-11-02T10:00"
})
public abstract class BaseIntegracaoTest {

    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final AtomicInteger SEQUENCIA = new AtomicInteger(100);

    @Autowired
    protected MembroService membroService;
    @Autowired
    protected PlanoService planoService;
    @Autowired
    protected EspacoService espacoService;

    /** Cria um membro novo (CPF e e-mail únicos) e, se informado, assina o plano. */
    protected Membro novoMembro(TipoPlano plano) {
        int n = SEQUENCIA.incrementAndGet();
        Membro m = membroService.cadastrar("Membro Teste " + n, gerarCpf(n), "membro" + n + "@teste.com",
                "(85) 90000-0000", "senha123");
        if (plano != null) {
            planoService.assinar(m, plano, DadosPagamento.pix());
        }
        return m;
    }

    protected Espaco novoEspaco(TipoEspaco tipo, int capacidade) {
        return espacoService.cadastrar(tipo, tipo.getDescricao() + " " + SEQUENCIA.incrementAndGet(), capacidade, "teste");
    }

    /** Gera um CPF válido a partir de um número (calcula os dígitos verificadores). */
    public static String gerarCpf(int semente) {
        String base = String.format("%09d", 100000000 + semente * 7919L % 899999999);
        base = base.substring(0, 9);
        base += digito(base);
        base += digito(base);
        return base;
    }

    private static int digito(String parcial) {
        int soma = 0;
        for (int i = 0; i < parcial.length(); i++) {
            soma += (parcial.charAt(i) - '0') * (parcial.length() + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    protected static DadosPagamento cartao(String numero) {
        DadosPagamento d = new DadosPagamento();
        d.setForma(br.com.openoffice.enums.FormaPagamento.CARTAO);
        d.setTitular("Cliente Teste");
        d.setNumeroCartao(numero);
        d.setValidade("12/30");
        d.setCvv("123");
        d.setParcelas(1);
        return d;
    }
}
