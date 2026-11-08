package com.maka.api.usuario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import com.maka.api.usuario.dto.UsuarioCreateRequest;
import com.maka.api.usuario.dto.UsuarioResponse;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    private Usuario usuario(Long id, String email, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNome("Fulano");
        u.setPerfil(perfil);
        return u;
    }

    @Test
    void adminCriaCozinheiroComSenhaBCrypt() {
        when(usuarioRepository.existsByEmail("coz@maka.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = usuarioService.criar(new UsuarioCreateRequest("Coz", "coz@maka.com", "123456", "kitchen"));

        assertEquals("kitchen", resp.role());
        assertEquals("ATIVO", resp.status());
    }

    @Test
    void naoCriaAdminNemClientePorAqui() {
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("X", "x@maka.com", "123456", "admin")));
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("X", "x@maka.com", "123456", "cliente")));
    }

    @Test
    void emailRepetidoDevolveConflito() {
        when(usuarioRepository.existsByEmail("coz@maka.com")).thenReturn(true);
        assertThrows(ConflitoException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("Coz", "coz@maka.com", "123456", "kitchen")));
    }

    @Test
    void exclusaoELogica() {
        Usuario u = usuario(2L, "coz@maka.com", Perfil.KITCHEN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(u));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.alterarStatus(2L, StatusUsuario.INATIVO, "admin@email.com");

        assertEquals(StatusUsuario.INATIVO, u.getStatus());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void naoDesativaAPropriaConta() {
        Usuario u = usuario(1L, "admin@email.com", Perfil.MANAGER);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.alterarStatus(1L, StatusUsuario.INATIVO, "ADMIN@email.com"));
    }
}
