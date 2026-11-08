package com.maka.api.usuario;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import com.maka.api.usuario.dto.UsuarioCreateRequest;
import com.maka.api.usuario.dto.UsuarioResponse;
import com.maka.api.usuario.dto.UsuarioUpdateRequest;

/** RF-041: o ADMIN cria e mantém os usuários internos. Clientes se cadastram sozinhos (/api/auth/register). */
@Service
@Transactional
public class UsuarioService {

    /** Perfis que o ADMIN pode criar/editar por aqui. */
    private static final Set<Perfil> PERFIS_INTERNOS = EnumSet.of(Perfil.MANAGER, Perfil.KITCHEN, Perfil.DELIVERY);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findByPerfilIn(EnumSet.of(Perfil.ADMIN, Perfil.MANAGER, Perfil.KITCHEN, Perfil.DELIVERY), pageable)
                .map(UsuarioResponse::from);
    }

    public UsuarioResponse criar(UsuarioCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(request.username().trim());
        usuario.setPerfil(perfilInterno(request.role()));
        usuario.setSenhaHash(passwordEncoder.encode(request.password()));
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarInterno(id);
        usuario.setNome(request.username().trim());
        usuario.setPerfil(perfilInterno(request.role()));
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.password()));
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /** RN06: exclusão e reativação são sempre mudança de status, nunca DELETE no banco. */
    public UsuarioResponse alterarStatus(Long id, StatusUsuario status, String emailLogado) {
        Usuario usuario = buscarInterno(id);
        if (status == StatusUsuario.INATIVO && usuario.getEmail().equalsIgnoreCase(emailLogado)) {
            throw new RegraNegocioException("Você não pode desativar a própria conta");
        }
        usuario.setStatus(status);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private Usuario buscarInterno(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado: " + id));
        if (usuario.getPerfil() == Perfil.CLIENTE) {
            throw new NoSuchElementException("Usuário não encontrado: " + id);
        }
        return usuario;
    }

    private Perfil perfilInterno(String role) {
        Perfil perfil;
        try {
            perfil = Perfil.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Perfil inválido: " + role + ". Use manager, kitchen ou delivery");
        }
        if (!PERFIS_INTERNOS.contains(perfil)) {
            throw new RegraNegocioException("Perfil inválido: " + role + ". Use manager, kitchen ou delivery");
        }
        return perfil;
    }
}
