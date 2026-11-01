package br.com.openoffice.service;

import br.com.openoffice.exception.AutenticacaoException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login com e-mail e senha.
 * - As senhas são guardadas apenas como hash BCrypt.
 * - Depois de 5 tentativas erradas, o e-mail fica bloqueado por 5 minutos.
 */
@Service
public class AutenticacaoService {

    public static final int MAX_TENTATIVAS = 5;
    public static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(5);
    public static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final AuditoriaService auditoria;
    private final Clock relogio;
    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();

    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder codificador,
                               AuditoriaService auditoria, Clock relogio) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public Usuario autenticar(String email, String senha) {
        String chave = email == null ? "" : email.trim().toLowerCase();
        if (chave.isBlank() || senha == null || senha.isBlank()) {
            throw new AutenticacaoException("Informe o e-mail e a senha.");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        Tentativas t = tentativas.computeIfAbsent(chave, k -> new Tentativas());
        if (t.bloqueadoAte != null && agora.isBefore(t.bloqueadoAte)) {
            long minutos = Math.max(1, Duration.between(agora, t.bloqueadoAte).toMinutes() + 1);
            throw new AutenticacaoException("Muitas tentativas erradas. Tente novamente em " + minutos
                    + " minuto" + (minutos > 1 ? "s" : "") + ".");
        }

        Usuario usuario = usuarios.buscarPorEmail(chave).orElse(null);
        if (usuario == null || !codificador.matches(senha, usuario.getSenhaHash())) {
            t.falhas++;
            if (t.falhas >= MAX_TENTATIVAS) {
                t.bloqueadoAte = agora.plus(TEMPO_BLOQUEIO);
                t.falhas = 0;
                auditoria.registrar(chave, "Login bloqueado por excesso de tentativas");
                throw new AutenticacaoException("Muitas tentativas erradas. O acesso foi bloqueado por "
                        + TEMPO_BLOQUEIO.toMinutes() + " minutos.");
            }
            int restantes = MAX_TENTATIVAS - t.falhas;
            throw new AutenticacaoException("E-mail ou senha incorretos. Tentativas restantes: " + restantes + ".");
        }
        if (!usuario.isAtivo()) {
            throw new AutenticacaoException("Esta conta está desativada. Procure a recepção.");
        }
        tentativas.remove(chave);
        auditoria.registrar(usuario.getEmail(), "Login realizado");
        return usuario;
    }

    public String gerarHash(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new DadosInvalidosException("A senha precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        return codificador.encode(senha);
    }

    public void alterarSenha(Usuario usuario, String senhaAtual, String novaSenha) {
        if (!codificador.matches(senhaAtual == null ? "" : senhaAtual, usuario.getSenhaHash())) {
            throw new DadosInvalidosException("A senha atual não confere.");
        }
        usuario.alterarSenhaHash(gerarHash(novaSenha));
        usuarios.salvar(usuario);
        auditoria.registrar(usuario.getEmail(), "Senha alterada");
    }

    private static final class Tentativas {
        private int falhas;
        private LocalDateTime bloqueadoAte;
    }
}
