package com.maka.api.auth;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.auth.dto.RegisterRequest;
import com.maka.api.model.Usuario;
import com.maka.api.model.Perfil;
import com.maka.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void deveIgnorarEspacosEMaiusculasNoEmailDoLogin() {
        LoginRequest req = new LoginRequest();
        req.setEmail("  Cozinha@Maka.com ");
        req.setPassword("123456");

        Usuario user = new Usuario();
        user.setId(2L);
        user.setEmail("cozinha@maka.com");
        user.setSenhaHash("hash");
        user.setPerfil(Perfil.KITCHEN);
        user.setNome("Cozinha");

        when(usuarioRepository.findByEmail("cozinha@maka.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateToken("cozinha@maka.com", "ROLE_KITCHEN")).thenReturn("tok");

        LoginResponse resp = authService.authenticate(req);

        assertEquals("kitchen", resp.getUser().getRole());
    }

    @Test
    void deveCadastrarClienteEDevolverToken() {
        RegisterRequest req = new RegisterRequest();
        req.setName("  Ana Souza ");
        req.setEmail(" Ana@Teste.com ");
        req.setPassword("senha123");

        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashBcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generateToken("ana@teste.com", "ROLE_CLIENTE")).thenReturn("tokenCliente");

        LoginResponse resp = authService.register(req);

        assertEquals("tokenCliente", resp.getToken());
        assertEquals("7", resp.getUser().getUid());
        assertEquals("client", resp.getUser().getRole());
        assertEquals("Ana Souza", resp.getUser().getUsername());

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertEquals(Perfil.CLIENTE, salvo.getValue().getPerfil());
        assertEquals("ana@teste.com", salvo.getValue().getEmail());
        assertEquals("hashBcrypt", salvo.getValue().getSenhaHash());
    }

    @Test
    void deveDevolver409QuandoEmailJaCadastrado() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Ana");
        req.setEmail("ana@teste.com");
        req.setPassword("senha123");

        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(req));
        assertEquals(409, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveDevolver409QuandoDoisCadastrosCorremJuntos() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Ana");
        req.setEmail("ana@teste.com");
        req.setPassword("senha123");

        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashBcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(req));
        assertEquals(409, ex.getStatusCode().value());
    }
}
