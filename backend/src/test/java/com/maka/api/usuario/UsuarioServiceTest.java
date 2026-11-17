package com.maka.api.usuario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import com.maka.api.usuario.dto.UsuarioCreateRequest;
import com.maka.api.usuario.dto.UsuarioResponse;
import com.maka.api.usuario.dto.UsuarioUpdateRequest;

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
        u.setSenhaHash("antigo");
        u.setPerfil(perfil);
        return u;
    }

    @Test
    void adminCriaCozinheiroComSenhaBCrypt() {
        when(usuarioRepository.existsByEmail("coz@maka.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = usuarioService.criar(new UsuarioCreateRequest("Coz", "  Coz@Maka.com ", "123456", "kitchen"));

        assertEquals("kitchen", resp.role());
        assertEquals("ATIVO", resp.status());
        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertEquals(Perfil.KITCHEN, salvo.getValue().getPerfil());
        assertEquals("hash", salvo.getValue().getSenhaHash());
        assertEquals("coz@maka.com", salvo.getValue().getEmail());
    }

    @Test
    void emailRepetidoDevolveConflito() {
        when(usuarioRepository.existsByEmail("coz@maka.com")).thenReturn(true);

        assertThrows(ConflitoException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("Coz", "coz@maka.com", "123456", "kitchen")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaAdminNemClientePorAqui() {
        when(usuarioRepository.existsByEmail("x@maka.com")).thenReturn(false);

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("X", "x@maka.com", "123456", "admin")));
        assertThrows(RegraNegocioException.class,
                () -> usuarioService.criar(new UsuarioCreateRequest("X", "x@maka.com", "123456", "client")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoDesativaAPropriaConta() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, "ger@maka.com", Perfil.MANAGER)));

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.alterarStatus(5L, StatusUsuario.INATIVO, "GER@maka.com"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desativaOutroUsuarioSemApagar() {
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario(6L, "coz@maka.com", Perfil.KITCHEN)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = usuarioService.alterarStatus(6L, StatusUsuario.INATIVO, "admin@email.com");

        assertEquals("INATIVO", resp.status());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void clienteNaoEhUsuarioInterno() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario(9L, "ana@teste.com", Perfil.CLIENTE)));

        assertThrows(NoSuchElementException.class,
                () -> usuarioService.atualizar(9L, new UsuarioUpdateRequest("Ana", "manager", null)));
    }

    @Test
    void contaAdminNaoEhAlteradaPorAqui() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "admin@email.com", Perfil.ADMIN)));

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.alterarStatus(1L, StatusUsuario.INATIVO, "outro@maka.com"));
    }

    @Test
    void atualizarSemSenhaMantemASenhaAtual() {
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario(6L, "coz@maka.com", Perfil.KITCHEN)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resp = usuarioService.atualizar(6L, new UsuarioUpdateRequest("Novo Nome", "delivery", ""));

        assertEquals("delivery", resp.role());
        assertEquals("Novo Nome", resp.username());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void senhaCurtaNaAtualizacaoDa400() {
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario(6L, "coz@maka.com", Perfil.KITCHEN)));

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.atualizar(6L, new UsuarioUpdateRequest("Coz", "kitchen", "123")));
        verify(usuarioRepository, never()).save(any());
    }
}
