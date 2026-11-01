package br.com.openoffice.model.pessoa;

import br.com.openoffice.enums.Perfil;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.model.plano.Assinatura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cliente do coworking. Guarda o histórico de assinaturas; o plano "ativo" é calculado
 * a partir da data, então um plano vence sozinho, sem precisar de rotina extra.
 */
public class Membro extends Usuario {

    private String telefone;
    private final List<Assinatura> assinaturas = new ArrayList<>();

    protected Membro() {
    }

    public Membro(String nome, String cpf, String email, String telefone, String senhaHash, LocalDateTime criadoEm) {
        super(nome, cpf, email, senhaHash, criadoEm);
        this.telefone = telefone;
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.MEMBRO;
    }

    @Override
    public String getPaginaInicial() {
        return "/membro/painel";
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void adicionarAssinatura(Assinatura assinatura) {
        assinaturas.add(assinatura);
    }

    /** Histórico de planos, do mais recente para o mais antigo. */
    public List<Assinatura> getAssinaturas() {
        List<Assinatura> copia = new ArrayList<>(assinaturas);
        copia.sort(Comparator.comparing(Assinatura::getInicio).reversed());
        return Collections.unmodifiableList(copia);
    }

    public Optional<Assinatura> getAssinaturaAtiva(LocalDate data) {
        return assinaturas.stream()
                .filter(a -> a.estaAtivaEm(data))
                .max(Comparator.comparing(Assinatura::getFim));
    }

    public boolean temPlanoAtivo(LocalDate data) {
        return getAssinaturaAtiva(data).isPresent();
    }

    public boolean isVip(LocalDate data) {
        return getAssinaturaAtiva(data).map(a -> a.getTipo().isVip()).orElse(false);
    }

    public Optional<TipoPlano> getPlanoAtual(LocalDate data) {
        return getAssinaturaAtiva(data).map(Assinatura::getTipo);
    }

    /** Último dia coberto por algum plano (inclusive planos já pagos que começam no futuro). */
    public Optional<LocalDate> getFimDaCobertura() {
        return assinaturas.stream().map(Assinatura::getFim).max(Comparator.naturalOrder());
    }

    /** Quantos dias à frente o membro pode reservar hoje (0 se não tiver plano). */
    public int getDiasAntecedenciaMaxima(LocalDate hoje) {
        return getPlanoAtual(hoje).map(TipoPlano::getDiasAntecedenciaReserva).orElse(0);
    }
}
