package br.com.openoffice.service;

import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.repository.CobrancaRepository;
import br.com.openoffice.repository.ReservaRepository;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.util.Validador;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Cadastro, edição e remoção de membros. */
@Service
public class MembroService {

    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final CobrancaRepository cobrancas;
    private final AutenticacaoService autenticacao;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;
    private final Clock relogio;

    public MembroService(UsuarioRepository usuarios, ReservaRepository reservas, CobrancaRepository cobrancas,
                         AutenticacaoService autenticacao, AuditoriaService auditoria,
                         NotificacaoService notificacoes, Clock relogio) {
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.cobrancas = cobrancas;
        this.autenticacao = autenticacao;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    public Membro cadastrar(String nome, String cpf, String email, String telefone, String senha) {
        validarCpf(cpf, null);
        validarEmailUnico(email, null);
        String hash = autenticacao.gerarHash(senha);
        Membro membro = new Membro(nome, Validador.formatarCpf(cpf), email, telefone, hash,
                LocalDateTime.now(relogio));
        usuarios.salvar(membro);
        notificacoes.notificar(membro.getId(), "Boas-vindas ao OpenOffice Coworking! Assine um plano para "
                + "começar a reservar espaços.");
        auditoria.registrar(membro.getEmail(), "Cadastro de membro " + membro.getId());
        return membro;
    }

    public Membro atualizar(String id, String nome, String cpf, String email, String telefone) {
        Membro membro = buscar(id);
        validarCpf(cpf, id);
        validarEmailUnico(email, id);
        membro.setNome(nome);
        membro.setDocumento(Validador.formatarCpf(cpf));
        membro.setEmail(email);
        membro.setTelefone(telefone);
        usuarios.salvar(membro);
        auditoria.registrar("admin", "Membro " + id + " atualizado");
        return membro;
    }

    public void alternarAtivo(String id) {
        Membro membro = buscar(id);
        if (membro.isAtivo()) {
            membro.desativar();
        } else {
            membro.ativar();
        }
        usuarios.salvar(membro);
        auditoria.registrar("admin", "Membro " + id + (membro.isAtivo() ? " reativado" : " desativado"));
    }

    /** Só remove membros sem histórico; quem já usou o espaço deve ser desativado (preserva relatórios). */
    public void remover(String id) {
        Membro membro = buscar(id);
        boolean temHistorico = !reservas.doMembro(id).isEmpty() || !cobrancas.doMembro(id).isEmpty();
        if (temHistorico) {
            throw new DadosInvalidosException(membro.getNome() + " tem reservas ou pagamentos no histórico e não "
                    + "pode ser removido. Use \"Desativar\" para bloquear o acesso.");
        }
        usuarios.remover(id);
        auditoria.registrar("admin", "Membro " + id + " removido");
    }

    public Membro buscar(String id) {
        return usuarios.buscarMembro(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro " + id + " não encontrado."));
    }

    public List<Membro> listar() {
        return usuarios.listarMembros();
    }

    public Usuario buscarUsuario(String id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    private void validarCpf(String cpf, String ignorarId) {
        if (!Validador.cpfValido(cpf)) {
            throw new DadosInvalidosException("CPF inválido. Confira os 11 dígitos.");
        }
        if (usuarios.existeCpf(cpf, ignorarId)) {
            throw new DadosInvalidosException("Já existe um membro cadastrado com este CPF.");
        }
    }

    private void validarEmailUnico(String email, String ignorarId) {
        if (!Validador.emailValido(email)) {
            throw new DadosInvalidosException("E-mail inválido. Exemplo: nome@dominio.com");
        }
        if (usuarios.existeEmail(email, ignorarId)) {
            throw new DadosInvalidosException("Este e-mail já está em uso.");
        }
    }
}
