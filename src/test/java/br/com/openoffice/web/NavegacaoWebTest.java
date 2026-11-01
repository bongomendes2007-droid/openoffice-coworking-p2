package br.com.openoffice.web;

import br.com.openoffice.BaseIntegracaoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa o controle de acesso e o tratamento de erros nas páginas. */
@AutoConfigureMockMvc
class NavegacaoWebTest extends BaseIntegracaoTest {

    @Autowired
    private MockMvc mvc;

    private MockHttpSession login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/login").param("email", email).param("senha", senha)).andReturn();
        return (MockHttpSession) r.getRequest().getSession();
    }

    @Test
    @DisplayName("Sem login, páginas restritas redirecionam para /login")
    void exigeLogin() throws Exception {
        mvc.perform(get("/membro/painel")).andExpect(redirectedUrl("/login?expirou"));
        mvc.perform(get("/admin/painel")).andExpect(redirectedUrl("/login?expirou"));
    }

    @Test
    @DisplayName("Login errado mostra a mensagem na própria página")
    void loginErrado() throws Exception {
        mvc.perform(post("/login").param("email", "admin@openoffice.com").param("senha", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("E-mail ou senha incorretos")));
    }

    @Test
    @DisplayName("Administrador acessa /admin mas não /membro (403)")
    void perfilAdmin() throws Exception {
        MockHttpSession sessao = login("admin@openoffice.com", "admin123");
        mvc.perform(get("/admin/painel").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/admin/relatorios").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/membro/painel").session(sessao)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Membro não acessa a área do administrador")
    void perfilMembro() throws Exception {
        String email = novoMembro(null).getEmail();
        MockHttpSession sessao = login(email, "senha123");
        mvc.perform(get("/membro/painel").session(sessao)).andExpect(status().isOk());
        mvc.perform(get("/admin/painel").session(sessao)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registro inexistente e parâmetro inválido mostram páginas amigáveis")
    void errosAmigaveis() throws Exception {
        MockHttpSession sessao = login("admin@openoffice.com", "admin123");
        mvc.perform(get("/admin/membros/U9999").session(sessao)).andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("não encontrado")));
        mvc.perform(get("/admin/painel").param("data", "31-02-2026").session(sessao)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Relatório exporta PDF válido")
    void exportaPdf() throws Exception {
        MockHttpSession sessao = login("admin@openoffice.com", "admin123");
        mvc.perform(get("/admin/relatorios/exportar").session(sessao)
                        .param("tipo", "ocupacao-sala").param("formato", "pdf")
                        .param("de", "2026-10-01").param("ate", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }
}
