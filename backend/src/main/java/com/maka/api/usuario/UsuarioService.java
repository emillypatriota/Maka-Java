package com.maka.api.usuario;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.auth.PerfilRoles;
import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.Perfil;
import com.maka.api.model.StatusUsuario;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import com.maka.api.usuario.dto.UsuarioCreateRequest;
import com.maka.api.usuario.dto.UsuarioResponse;
import com.maka.api.usuario.dto.UsuarioUpdateRequest;

/**
 * RF-041: o ADMIN cria e mantém os usuários internos (gerente, cozinheiro, entregador).
 * Cliente se cadastra sozinho em /api/auth/register e não aparece aqui.
 * Contas ADMIN aparecem na lista, mas não são alteradas por esta tela.
 */
@Service
public class UsuarioService {

    // Perfis que o ADMIN pode criar e editar por aqui
    static final Set<Perfil> PERFIS_EDITAVEIS = EnumSet.of(Perfil.MANAGER, Perfil.KITCHEN, Perfil.DELIVERY);

    // Perfis que aparecem na lista (todo mundo menos cliente)
    private static final Set<Perfil> PERFIS_INTERNOS = EnumSet.of(
            Perfil.ADMIN, Perfil.MANAGER, Perfil.KITCHEN, Perfil.DELIVERY);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findByPerfilIn(PERFIS_INTERNOS, pageable).map(UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> PERFIS_INTERNOS.contains(u.getPerfil()))
                .orElseThrow(() -> naoEncontrado(id));
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(request.username().trim());
        usuario.setPerfil(perfilEditavel(request.role()));
        usuario.setSenhaHash(passwordEncoder.encode(request.password()));
        usuario.setStatus(StatusUsuario.ATIVO);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEditavel(id);
        usuario.setNome(request.username().trim());
        usuario.setPerfil(perfilEditavel(request.role()));

        String senha = request.password();
        if (senha != null && !senha.isBlank()) {
            if (senha.length() < 6) {
                throw new IllegalArgumentException("A senha deve ter entre 6 e 72 caracteres");
            }
            usuario.setSenhaHash(passwordEncoder.encode(senha));
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    // RN06: excluir e reativar são só troca de status, nunca DELETE no banco
    @Transactional
    public UsuarioResponse alterarStatus(Long id, StatusUsuario status, String emailLogado) {
        Usuario usuario = buscarEditavel(id);
        if (status == StatusUsuario.INATIVO && usuario.getEmail().equalsIgnoreCase(emailLogado)) {
            throw new RegraNegocioException("Você não pode desativar a própria conta");
        }
        usuario.setStatus(status);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    private Usuario buscarEditavel(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> naoEncontrado(id));
        if (usuario.getPerfil() == Perfil.CLIENTE) {
            // Cliente não é usuário interno: para esta tela ele não existe
            throw naoEncontrado(id);
        }
        if (usuario.getPerfil() == Perfil.ADMIN) {
            throw new RegraNegocioException("Contas de administrador não podem ser alteradas por aqui");
        }
        return usuario;
    }

    private Perfil perfilEditavel(String role) {
        Perfil perfil = PerfilRoles.perfilDoFrontRole(role);
        if (perfil == null || !PERFIS_EDITAVEIS.contains(perfil)) {
            throw new RegraNegocioException("Perfil inválido: " + role + ". Use manager, kitchen ou delivery");
        }
        return perfil;
    }

    private NoSuchElementException naoEncontrado(Long id) {
        return new NoSuchElementException("Usuário não encontrado: " + id);
    }
}
