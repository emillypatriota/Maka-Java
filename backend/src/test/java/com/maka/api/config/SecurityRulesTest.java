package com.maka.api.config;

import com.maka.api.auth.AuthController;
import com.maka.api.auth.AuthService;
import com.maka.api.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Confere a tabela de permissões do SecurityConfig sem precisar de banco.
 * Só o AuthController existe neste teste: para as outras rotas, "liberado" aparece como
 * 404 (rota sem controller aqui) e "bloqueado" como 401 (sem login) ou 403 (perfil errado).
 * Mudou uma regra no SecurityConfig? Ajuste a tabela abaixo no mesmo PR.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public class SecurityRulesTest {

    private static final List<String> LOGADOS = List.of("CLIENTE", "KITCHEN", "DELIVERY", "MANAGER", "ADMIN");
    private static final List<String> PAINEL = List.of("KITCHEN", "DELIVERY", "MANAGER", "ADMIN");
    private static final List<String> GERENCIA = List.of("MANAGER", "ADMIN");

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Value("${maka.jwt.secret}") private String segredo;
    @MockitoBean private AuthService authService;

    // perfis == null  -> rota pública | senão: lista de perfis logados que podem entrar
    private record Rota(HttpMethod metodo, String caminho, List<String> perfis) {
        boolean publica() { return perfis == null; }
    }

    private static Rota publica(HttpMethod m, String c) { return new Rota(m, c, null); }
    private static Rota rota(HttpMethod m, String c, List<String> perfis) { return new Rota(m, c, perfis); }

    private static List<Rota> rotas() {
        List<Rota> r = new ArrayList<>();
        // Públicas
        r.add(publica(HttpMethod.POST, "/api/auth/login"));
        r.add(publica(HttpMethod.POST, "/api/auth/register"));
        r.add(publica(HttpMethod.GET, "/api/cardapio"));
        r.add(publica(HttpMethod.GET, "/api/cardapio/1"));
        r.add(publica(HttpMethod.GET, "/api/cardapio/categorias"));
        r.add(publica(HttpMethod.GET, "/v3/api-docs"));
        r.add(publica(HttpMethod.GET, "/swagger-ui.html"));
        // Qualquer logado
        r.add(rota(HttpMethod.GET, "/api/auth/me", LOGADOS));
        r.add(rota(HttpMethod.GET, "/api/pedidos/1/status", LOGADOS));
        r.add(rota(HttpMethod.GET, "/api/rota-nova-sem-regra", LOGADOS));
        // Cliente
        r.add(rota(HttpMethod.POST, "/api/pedidos", List.of("CLIENTE", "MANAGER", "ADMIN")));
        r.add(rota(HttpMethod.GET, "/api/pedidos/meus", List.of("CLIENTE")));
        // Cozinha / entregador
        r.add(rota(HttpMethod.GET, "/api/pedidos", PAINEL));
        r.add(rota(HttpMethod.PATCH, "/api/pedidos/1/status", PAINEL));
        r.add(rota(HttpMethod.PATCH, "/api/pedidos/1/entregador", List.of("DELIVERY", "MANAGER", "ADMIN")));
        r.add(rota(HttpMethod.GET, "/api/admin/pedidos", PAINEL));
        r.add(rota(HttpMethod.PATCH, "/api/admin/pedidos/1/status", PAINEL));
        // Gerência
        r.add(rota(HttpMethod.POST, "/api/cardapio", GERENCIA));
        r.add(rota(HttpMethod.PUT, "/api/cardapio/1", GERENCIA));
        r.add(rota(HttpMethod.PATCH, "/api/cardapio/1/ativo", GERENCIA));
        r.add(rota(HttpMethod.DELETE, "/api/cardapio/1", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/funcionarios", GERENCIA));
        r.add(rota(HttpMethod.POST, "/api/funcionarios", GERENCIA));
        r.add(rota(HttpMethod.DELETE, "/api/funcionarios/1", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/insights/resumo", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/admin/dashboard/resumo", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/admin/ingredientes", GERENCIA));
        r.add(rota(HttpMethod.POST, "/api/admin/ingredientes", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/admin/pratos", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/admin/categorias", GERENCIA));
        r.add(rota(HttpMethod.POST, "/api/admin/categorias", GERENCIA));
        r.add(rota(HttpMethod.GET, "/api/admin/pratos/1/ficha-tecnica", GERENCIA));
        r.add(rota(HttpMethod.PUT, "/api/admin/pratos/1/ficha-tecnica", GERENCIA));
        r.add(rota(HttpMethod.PUT, "/api/admin/pratos/1", GERENCIA));
        r.add(rota(HttpMethod.POST, "/api/admin/funcionarios", GERENCIA));
        r.add(rota(HttpMethod.PATCH, "/api/admin/funcionarios/1/status", GERENCIA));
        r.add(rota(HttpMethod.PATCH, "/api/admin/pedidos/1/cancelar", PAINEL));
        r.add(rota(HttpMethod.GET, "/api/admin/fornecedores", GERENCIA));
        r.add(rota(HttpMethod.POST, "/api/admin/pratos", GERENCIA));
        // Só admin
        r.add(rota(HttpMethod.GET, "/api/admin/usuarios", List.of("ADMIN")));
        r.add(rota(HttpMethod.POST, "/api/admin/usuarios", List.of("ADMIN")));
        return r;
    }

    private int status(HttpMethod metodo, String caminho, String authorization) throws Exception {
        MockHttpServletRequestBuilder req = request(metodo, caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        if (authorization != null) {
            req.header("Authorization", authorization);
        }
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    private String bearer(String role) {
        return "Bearer " + jwtService.generateToken("teste@maka.com", "ROLE_" + role);
    }

    @Test
    void tabelaDePermissoesPorRota() throws Exception {
        List<String> falhas = new ArrayList<>();

        for (Rota rota : rotas()) {
            String nome = rota.metodo() + " " + rota.caminho();

            // Sem login
            int anonimo = status(rota.metodo(), rota.caminho(), null);
            if (rota.publica()) {
                if (anonimo == 401 || anonimo == 403) falhas.add(nome + " deveria ser pública, veio " + anonimo);
            } else if (anonimo != 401) {
                falhas.add(nome + " sem login deveria dar 401, veio " + anonimo);
            }

            // Cada perfil
            for (String perfil : LOGADOS) {
                int s = status(rota.metodo(), rota.caminho(), bearer(perfil));
                boolean liberado = rota.publica() || rota.perfis().contains(perfil);
                if (liberado && (s == 401 || s == 403)) {
                    falhas.add(nome + " como " + perfil + " deveria passar, veio " + s);
                }
                if (!liberado && s != 403) {
                    falhas.add(nome + " como " + perfil + " deveria dar 403, veio " + s);
                }
            }
        }

        assertTrue(falhas.isEmpty(), "Regras de acesso fora do esperado:\n" + String.join("\n", falhas));
    }

    @Test
    void tokenComPerfilDesconhecidoNaoAutentica() throws Exception {
        String alien = "Bearer " + jwtService.generateToken("x@maka.com", "ROLE_ALIEN");
        assertEquals(401, status(HttpMethod.GET, "/api/auth/me", alien));
    }

    @Test
    void tokenLixoOuAdulteradoDa401() throws Exception {
        assertEquals(401, status(HttpMethod.GET, "/api/auth/me", "Bearer abc.def.ghi"));
        assertEquals(401, status(HttpMethod.GET, "/api/auth/me", bearer("MANAGER") + "x"));
    }

    @Test
    void tokenAssinadoComOutraChaveDa401() throws Exception {
        JwtService falso = new JwtService();
        ReflectionTestUtils.setField(falso, "secretKeyString", "outra-chave-secreta-com-mais-de-32-caracteres-aqui");
        ReflectionTestUtils.setField(falso, "jwtExpirationInMs", 3600000L);
        String token = "Bearer " + falso.generateToken("gerente@maka.com", "ROLE_MANAGER");
        assertEquals(401, status(HttpMethod.GET, "/api/funcionarios", token));
    }

    @Test
    void tokenExpiradoDa401() throws Exception {
        JwtService vencido = new JwtService();
        ReflectionTestUtils.setField(vencido, "secretKeyString", segredo);
        ReflectionTestUtils.setField(vencido, "jwtExpirationInMs", -60000L);
        String token = "Bearer " + vencido.generateToken("gerente@maka.com", "ROLE_MANAGER");
        assertEquals(401, status(HttpMethod.GET, "/api/funcionarios", token));
    }

    @Test
    void errosSaemNoFormatoPadraoStatusMessage() throws Exception {
        String sem401 = mvc.perform(request(HttpMethod.GET, "/api/funcionarios"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(sem401.contains("\"status\":401") && sem401.contains("\"message\""), sem401);

        String sem403 = mvc.perform(request(HttpMethod.GET, "/api/funcionarios").header("Authorization", bearer("CLIENTE")))
                .andReturn().getResponse().getContentAsString();
        assertTrue(sem403.contains("\"status\":403") && sem403.contains("\"message\""), sem403);
    }
}
