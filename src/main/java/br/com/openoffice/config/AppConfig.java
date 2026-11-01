package br.com.openoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Objetos compartilhados pela aplicação. */
@Configuration
public class AppConfig {

    /**
     * Relógio do sistema no fuso de Brasília. Os serviços pedem a hora a este objeto em vez de
     * chamar LocalDateTime.now() diretamente; assim os testes podem usar uma data fixa.
     */
    @Bean
    public Clock relogio(@Value("${openoffice.relogio-fixo:}") String dataFixa) {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        if (dataFixa == null || dataFixa.isBlank()) {
            return Clock.system(fuso);
        }
        // Opcional: "parar" o relógio numa data (útil para testes e para ensaiar a apresentação).
        return Clock.fixed(LocalDateTime.parse(dataFixa).atZone(fuso).toInstant(), fuso);
    }

    /** BCrypt: gera um hash diferente a cada senha (com "sal"), lento de propósito contra força bruta. */
    @Bean
    public PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }
}
