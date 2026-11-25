package com.maka.api.auth;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// RN06 no login: usuário desativado pelo ADMIN não entra e perde a sessão no /me
@ExtendWith(MockitoExtension.class)
class AuthServiceStatusTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    private Usuario cozinheiro(StatusUsuario status) {
        Usuario u = new Usuario();
        u.setId(4L);
        u.setEmail("x@maka.com");
        u.setNome("Chef");
        u.setSenhaHash("hash");
        u.setPerfil(Perfil.KITCHEN);
        u.setStatus(status);
        return u;
    }

    private LoginRequest login() {
        LoginRequest req = new LoginRequest();
        req.setEmail("x@maka.com");
        req.setPassword("123456");
        return req;
    }

    @Test
    void usuarioInativoNaoLoga() {
        when(usuarioRepository.findByEmail("x@maka.com")).thenReturn(Optional.of(cozinheiro(StatusUsuario.INATIVO)));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(login()));
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void usuarioAtivoContinuaLogando() {
        when(usuarioRepository.findByEmail("x@maka.com")).thenReturn(Optional.of(cozinheiro(StatusUsuario.ATIVO)));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateToken("x@maka.com", "ROLE_KITCHEN")).thenReturn("tok");

        LoginResponse resp = authService.authenticate(login());

        assertEquals("tok", resp.getToken());
        assertEquals("kitchen", resp.getUser().getRole());
    }

    @Test
    void usuarioInativoPerdeSessaoNoMe() {
        when(usuarioRepository.findByEmail("x@maka.com")).thenReturn(Optional.of(cozinheiro(StatusUsuario.INATIVO)));

        assertThrows(BadCredentialsException.class, () -> authService.buscarPorEmail("x@maka.com"));
    }
}
