package com.maka.api.auth;

import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.config.JwtAccessDeniedHandler;
import com.maka.api.config.JwtAuthenticationEntryPoint;
import com.maka.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public class AuthControllerTest {

    private static final String BODY_OK = "{\"name\":\"Ana Souza\",\"email\":\"ana@teste.com\",\"password\":\"senha123\"}";

    @Autowired private MockMvc mvc;
    @MockitoBean private AuthService authService;

    @Test
    void registerDevolve201ComTokenEUsuario() throws Exception {
        when(authService.register(any())).thenReturn(
                new LoginResponse("tokenCliente", new LoginResponse.UserDto("7", "ana@teste.com", "client", "Ana Souza")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tokenCliente"))
                .andExpect(jsonPath("$.user.uid").value("7"))
                .andExpect(jsonPath("$.user.role").value("client"))
                .andExpect(jsonPath("$.user.username").value("Ana Souza"));
    }

    @Test
    void registerComDadosInvalidosDevolve400ComCampos() throws Exception {
        String invalido = "{\"name\":\"\",\"email\":\"nao-e-email\",\"password\":\"123\"}";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.name").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void registerComEmailRepetidoDevolve409ComMensagem() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY_OK))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }
}
