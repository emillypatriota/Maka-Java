package com.maka.api.auth;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.auth.dto.RegisterRequest;
import com.maka.api.model.Perfil;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public LoginResponse authenticate(LoginRequest request) {
        // Busca obrigatória no banco real mapeado pelo grupo
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(request.getEmail()))
                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos"));

        // Validação estrita da senha criptografada via BCrypt usando o campo senhaHash
        if (!passwordEncoder.matches(request.getPassword(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }

        return montarResposta(usuario);
    }

    // POST /api/auth/register: cadastro de CLIENTE (RN10: e-mail repetido devolve 409)
    // Sem @Transactional de propósito: o save() abre a própria transação e o erro de
    // chave única (corrida entre dois cadastros) chega aqui para virar 409 também.
    public LoginResponse register(RegisterRequest request) {
        String email = normalizarEmail(request.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw emailJaCadastrado();
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(request.getName().trim());
        usuario.setSenhaHash(passwordEncoder.encode(request.getPassword()));
        usuario.setPerfil(Perfil.CLIENTE);

        try {
            usuario = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw emailJaCadastrado();
        }

        // Já devolve o token: o cliente fica logado logo depois de se cadastrar
        return montarResposta(usuario);
    }

    // Usado pelo GET /api/auth/me: devolve o usuario do token no mesmo formato do login
    public LoginResponse.UserDto buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        return paraUserDto(usuario);
    }

    private LoginResponse montarResposta(Usuario usuario) {
        // O perfil vira ROLE_<PERFIL do SRS> no token e minúsculo no JSON do front
        String token = jwtService.generateToken(usuario.getEmail(), PerfilRoles.authority(usuario.getPerfil()));
        return new LoginResponse(token, paraUserDto(usuario));
    }

    private LoginResponse.UserDto paraUserDto(Usuario usuario) {
        return new LoginResponse.UserDto(
                String.valueOf(usuario.getId()),
                usuario.getEmail(),
                PerfilRoles.frontRole(usuario.getPerfil()),
                usuario.getNome()
        );
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    // ResponseStatusException cai no handler genérico do GlobalExceptionHandler
    // (status 409 + message). Não precisa mexer no arquivo da Base.
    private ResponseStatusException emailJaCadastrado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    }
}
