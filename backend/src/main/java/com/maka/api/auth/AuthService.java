package com.maka.api.auth;

import com.maka.api.auth.dto.LoginRequest;
import com.maka.api.auth.dto.LoginResponse;
import com.maka.api.model.Usuario;
import com.maka.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos"));

        // Validação estrita da senha criptografada via BCrypt usando o campo senhaHash
        if (!passwordEncoder.matches(request.getPassword(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }

        // Conversão de Roles para manter compatibilidade com o Spring Security e o Front
        String frontRole = usuario.getPerfil().name().toLowerCase();
        String springRole = "ROLE_" + usuario.getPerfil().name();

        String token = jwtService.generateToken(usuario.getEmail(), springRole);

        // Montagem do DTO composto exatamente sob demanda do Front-end
        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                String.valueOf(usuario.getId()),
                usuario.getEmail(),
                frontRole,
                usuario.getNome()
        );

        return new LoginResponse(token, userDto);
    }

    // Usado pelo GET /api/auth/me: devolve o usuario do token no mesmo formato do login
    public LoginResponse.UserDto buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        return new LoginResponse.UserDto(
                String.valueOf(usuario.getId()),
                usuario.getEmail(),
                usuario.getPerfil().name().toLowerCase(),
                usuario.getNome()
        );
    }
}
