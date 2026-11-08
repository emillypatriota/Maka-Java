package com.maka.api.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.auth.dto.RegisterRequest;
import com.maka.api.exception.ConflitoException;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void cadastraClienteComSenhaCriptografadaEDevolveToken() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generateToken("ana@teste.com", "ROLE_CLIENTE")).thenReturn("tok");

        LoginResponse resp = authService.register(new RegisterRequest("Ana", "  Ana@Teste.com ", "123456"));

        assertEquals("tok", resp.getToken());
        assertEquals("cliente", resp.getUser().getRole());
        assertEquals("7", resp.getUser().getUid());
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertEquals(Perfil.CLIENTE, salvo.getValue().getPerfil());
        assertEquals("hash", salvo.getValue().getSenhaHash());
        assertEquals("ana@teste.com", salvo.getValue().getEmail());
    }

    @Test
    void emailRepetidoDevolveConflito() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThrows(ConflitoException.class,
                () -> authService.register(new RegisterRequest("Ana", "ana@teste.com", "123456")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInativoNaoLoga() {
        Usuario u = new Usuario();
        u.setEmail("x@maka.com");
        u.setSenhaHash("hash");
        u.setPerfil(Perfil.KITCHEN);
        u.setStatus(StatusUsuario.INATIVO);
        when(usuarioRepository.findByEmail("x@maka.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("x@maka.com");
        req.setPassword("123456");

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
    }
}
