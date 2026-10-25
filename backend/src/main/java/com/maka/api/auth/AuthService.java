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
        // Busca o usuário correto no banco pelo e-mail
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos"));

        // Compara a senha digitada com a criptografada do banco
        if (!passwordEncoder.matches(request.getPassword(), usuario.getSenha())) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }

        // Pega o perfil (MANAGER, KITCHEN, DELIVERY) e formata para o Front e para o Spring
        String frontRole = usuario.getPerfil().name().toLowerCase();
        String springRole = "ROLE_" + usuario.getPerfil().name();

        // Gera o token com o cargo correto
        String token = jwtService.generateToken(usuario.getEmail(), springRole);

        // Monta o objeto 'user' exatamente como a Dupla Front/Gestão exigiu
        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
                String.valueOf(usuario.getId()),
                usuario.getEmail(),
                frontRole,
                usuario.getNome()
        );

        return new LoginResponse(token, userDto);
    }
}
