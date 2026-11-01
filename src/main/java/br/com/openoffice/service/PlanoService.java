package br.com.openoffice.service;

import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.plano.Assinatura;
import br.com.openoffice.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Assinatura e renovação de planos. */
@Service
public class PlanoService {

    /** Limite para comprar planos antecipados (evita pagar meses e meses adiante por engano). */
    public static final int MAX_DIAS_COBERTURA_FUTURA = 60;

    private final UsuarioRepository usuarios;
    private final FinanceiroService financeiro;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public PlanoService(UsuarioRepository usuarios, FinanceiroService financeiro, NotificacaoService notificacoes,
                        AuditoriaService auditoria, Clock relogio) {
        this.usuarios = usuarios;
        this.financeiro = financeiro;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    /** Data em que um novo plano começaria: hoje, ou o dia seguinte ao fim do plano atual. */
    public LocalDate inicioDoProximoPlano(Membro membro) {
        LocalDate hoje = LocalDate.now(relogio);
        return membro.getFimDaCobertura()
                .filter(fim -> !fim.isBefore(hoje))
                .map(fim -> fim.plusDays(1))
                .orElse(hoje);
    }

    public Assinatura assinar(Membro membro, TipoPlano tipo, DadosPagamento pagamento) {
        if (tipo == null) {
            throw new DadosInvalidosException("Escolha um plano.");
        }
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate inicio = inicioDoProximoPlano(membro);
        if (inicio.isAfter(hoje.plusDays(MAX_DIAS_COBERTURA_FUTURA))) {
            throw new DadosInvalidosException("Você já tem planos pagos até "
                    + inicio.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ". Renove mais perto do vencimento.");
        }
        Assinatura assinatura = new Assinatura(membro.getId(), tipo, inicio, LocalDateTime.now(relogio));
        financeiro.cobrarEPagar(membro, assinatura, pagamento);
        membro.adicionarAssinatura(assinatura);
        usuarios.salvar(membro);
        notificacoes.notificar(membro.getId(), "Plano " + tipo.getDescricao() + " contratado: "
                + assinatura.getDescricaoCobranca().replace("Plano " + tipo.getDescricao() + " ", "") + ".");
        auditoria.registrar(membro.getEmail(), "Assinou o plano " + tipo.getDescricao());
        return assinatura;
    }

    /** Dias até o fim do plano atual (ou -1 se não houver plano ativo). */
    public long diasParaVencer(Membro membro) {
        LocalDate hoje = LocalDate.now(relogio);
        return membro.getAssinaturaAtiva(hoje)
                .map(a -> java.time.temporal.ChronoUnit.DAYS.between(hoje, membro.getFimDaCobertura().orElse(a.getFim())))
                .orElse(-1L);
    }
}
