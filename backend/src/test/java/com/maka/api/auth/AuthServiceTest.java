package com.maka.api.auth;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.model.Usuario;
import com.maka.api.model.Perfil;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void deveAutenticarComSucesso() {
        LoginRequest req = new LoginRequest();
        req.setEmail("gerente@maka.com");
        req.setPassword("123456");

        Usuario user = new Usuario();
        user.setId(1L);
        user.setEmail("gerente@maka.com");
        user.setSenhaHash("senhaCriptografada");
        user.setPerfil(Perfil.MANAGER);
        user.setNome("Gabriel");

        when(usuarioRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getSenhaHash())).thenReturn(true);
        when(jwtService.generateToken(user.getEmail(), "ROLE_MANAGER")).thenReturn("tokenValido");

        LoginResponse resp = authService.authenticate(req);

        assertNotNull(resp);
        assertEquals("tokenValido", resp.getToken());
        assertEquals("manager", resp.getUser().getRole());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaIncorreta() {
        LoginRequest req = new LoginRequest();
        req.setEmail("gerente@maka.com");
        req.setPassword("errada");

        Usuario user = new Usuario();
        user.setSenhaHash("senhaCriptografada");

        when(usuarioRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getSenhaHash())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }
}
