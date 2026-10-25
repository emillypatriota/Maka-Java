[0001-feat-auth-login-com-JWT-e-prote-o-de-rotas-por-perfi (1).patch](https://github.com/user-attachments/files/33919899/0001-feat-auth-login-com-JWT-e-prote-o-de-rotas-por-perfi.1.patch)
From 154a4a0e82f37a32908d46bfa81726e8df00a337 Mon Sep 17 00:00:00 2001
From: Claude <vitorroqueemidio@gmail.com>
Date: Sun, 25 Oct 2026 02:47:43 -0300
Subject: [PATCH] =?UTF-8?q?feat(auth):=20login=20com=20JWT=20e=20prote?=
 =?UTF-8?q?=C3=A7=C3=A3o=20de=20rotas=20por=20perfil?=
MIME-Version: 1.0
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

- AuthController/AuthService/JwtService com POST /api/auth/login
- JwtFilter e JwtAuthenticationEntryPoint no SecurityFilterChain
- Rotas por perfil: MANAGER (admin/funcionarios), KITCHEN (cozinha), DELIVERY (entregas)
- Cardápio (GET) e criação de pedido continuam públicos
- application.properties: maka.jwt.expiration e CORS com localhost:3000

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01QVJtLjPrDr2fZUMkoowThi
---
 .../com/maka/api/auth/AuthController.java     | 31 +++++++++
 .../java/com/maka/api/auth/AuthService.java   | 50 ++++++++++++++
 .../com/maka/api/auth/AuthServiceTest.java    | 65 ++++++++++++++++++
 .../java/com/maka/api/auth/JwtService.java    | 66 +++++++++++++++++++
 .../com/maka/api/auth/JwtServiceTest.java     | 34 ++++++++++
 .../com/maka/api/auth/dto/LoginRequest.java   | 11 ++++
 .../com/maka/api/auth/dto/LoginResponse.java  | 35 ++++++++++
 .../com/maka/api/auth/dto/UserResponse.java   | 13 ++++
 .../config/JwtAuthenticationEntryPoint.java   | 16 +++++
 .../java/com/maka/api/config/JwtFilter.java   | 35 ++++++++++
 .../com/maka/api/config/SecurityConfig.java   | 29 ++++++--
 .../src/main/resources/application.properties |  8 +--
 12 files changed, 382 insertions(+), 11 deletions(-)
 create mode 100644 backend/src/main/java/com/maka/api/auth/AuthController.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/AuthService.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/AuthServiceTest.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/JwtService.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/JwtServiceTest.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/dto/LoginRequest.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/dto/LoginResponse.java
 create mode 100644 backend/src/main/java/com/maka/api/auth/dto/UserResponse.java
 create mode 100644 backend/src/main/java/com/maka/api/config/JwtAuthenticationEntryPoint.java
 create mode 100644 backend/src/main/java/com/maka/api/config/JwtFilter.java

diff --git a/backend/src/main/java/com/maka/api/auth/AuthController.java b/backend/src/main/java/com/maka/api/auth/AuthController.java
new file mode 100644
index 0000000..7d10bc5
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/AuthController.java
@@ -0,0 +1,31 @@
+package com.maka.api.auth;
+
+import com.maka.api.auth.dto.LoginRequest;
+import com.maka.api.auth.dto.LoginResponse;
+import com.maka.api.auth.dto.UserResponse;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.http.ResponseEntity;
+import org.springframework.security.core.context.SecurityContextHolder;
+import org.springframework.web.bind.annotation.*;
+
+@RestController
+@RequestMapping("/api/auth")
+public class AuthController {
+
+    @Autowired
+    private AuthService authService;
+
+    @PostMapping("/login")
+    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
+        return ResponseEntity.ok(authService.authenticate(request));
+    }
+
+    @GetMapping("/me")
+    public ResponseEntity<UserResponse> me() {
+        var authentication = SecurityContextHolder.getContext().getAuthentication();
+        String email = authentication.getName();
+        String role = authentication.getAuthorities().iterator().next().getAuthority();
+        
+        return ResponseEntity.ok(new UserResponse(email, role));
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/AuthService.java b/backend/src/main/java/com/maka/api/auth/AuthService.java
new file mode 100644
index 0000000..26bf9ac
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/AuthService.java
@@ -0,0 +1,50 @@
+package com.maka.api.auth;
+
+import com.maka.api.auth.dto.LoginRequest;
+import com.maka.api.auth.dto.LoginResponse;
+import com.maka.api.model.Usuario;
+import com.maka.api.repository.UsuarioRepository;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.security.authentication.BadCredentialsException;
+import org.springframework.security.crypto.password.PasswordEncoder;
+import org.springframework.stereotype.Service;
+
+@Service
+public class AuthService {
+
+    @Autowired
+    private UsuarioRepository usuarioRepository;
+
+    @Autowired
+    private PasswordEncoder passwordEncoder;
+
+    @Autowired
+    private JwtService jwtService;
+
+    public LoginResponse authenticate(LoginRequest request) {
+        // Busca obrigatória no banco real mapeado pelo grupo
+        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
+                .orElseThrow(() -> new BadCredentialsException("Usuário ou senha inválidos"));
+
+        // Validação estrita da senha criptografada via BCrypt usando o campo senhaHash
+        if (!passwordEncoder.matches(request.getPassword(), usuario.getSenhaHash())) {
+            throw new BadCredentialsException("Usuário ou senha inválidos");
+        }
+
+        // Conversão de Roles para manter compatibilidade com o Spring Security e o Front
+        String frontRole = usuario.getPerfil().name().toLowerCase();
+        String springRole = "ROLE_" + usuario.getPerfil().name();
+
+        String token = jwtService.generateToken(usuario.getEmail(), springRole);
+
+        // Montagem do DTO composto exatamente sob demanda do Front-end
+        LoginResponse.UserDto userDto = new LoginResponse.UserDto(
+                String.valueOf(usuario.getId()),
+                usuario.getEmail(),
+                frontRole,
+                usuario.getNome()
+        );
+
+        return new LoginResponse(token, userDto);
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/AuthServiceTest.java b/backend/src/main/java/com/maka/api/auth/AuthServiceTest.java
new file mode 100644
index 0000000..bf701c6
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/AuthServiceTest.java
@@ -0,0 +1,65 @@
+package com.maka.api.auth;
+
+import com.maka.api.auth.dto.LoginRequest;
+import com.maka.api.auth.dto.LoginResponse;
+import com.maka.api.model.Usuario;
+import com.maka.api.model.Perfil;
+import com.maka.api.repository.UsuarioRepository;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+import org.springframework.security.authentication.BadCredentialsException;
+import org.springframework.security.crypto.password.PasswordEncoder;
+import java.util.Optional;
+import static org.junit.jupiter.api.Assertions.*;
+import static org.mockito.Mockito.*;
+
+@ExtendWith(MockitoExtension.class)
+public class AuthServiceTest {
+
+    @Mock private UsuarioRepository usuarioRepository;
+    @Mock private PasswordEncoder passwordEncoder;
+    @Mock private JwtService jwtService;
+    @InjectMocks private AuthService authService;
+
+    @Test
+    void deveAutenticarComSucesso() {
+        LoginRequest req = new LoginRequest();
+        req.setEmail("gerente@maka.com");
+        req.setPassword("123456");
+
+        Usuario user = new Usuario();
+        user.setId(1L);
+        user.setEmail("gerente@maka.com");
+        user.setSenhaHash("senhaCriptografada");
+        user.setPerfil(Perfil.MANAGER);
+        user.setNome("Gabriel");
+
+        when(usuarioRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
+        when(passwordEncoder.matches(req.getPassword(), user.getSenhaHash())).thenReturn(true);
+        when(jwtService.generateToken(user.getEmail(), "ROLE_MANAGER")).thenReturn("tokenValido");
+
+        LoginResponse resp = authService.authenticate(req);
+
+        assertNotNull(resp);
+        assertEquals("tokenValido", resp.getToken());
+        assertEquals("manager", resp.getUser().getRole());
+    }
+
+    @Test
+    void deveLancarExcecaoQuandoSenhaIncorreta() {
+        LoginRequest req = new LoginRequest();
+        req.setEmail("gerente@maka.com");
+        req.setPassword("errada");
+
+        Usuario user = new Usuario();
+        user.setSenhaHash("senhaCriptografada");
+
+        when(usuarioRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
+        when(passwordEncoder.matches(req.getPassword(), user.getSenhaHash())).thenReturn(false);
+
+        assertThrows(BadCredentialsException.class, () -> authService.authenticate(req));
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/JwtService.java b/backend/src/main/java/com/maka/api/auth/JwtService.java
new file mode 100644
index 0000000..58bef46
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/JwtService.java
@@ -0,0 +1,66 @@
+package com.maka.api.auth;
+
+import io.jsonwebtoken.Claims;
+import io.jsonwebtoken.Jwts;
+import io.jsonwebtoken.security.Keys;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.stereotype.Service;
+import javax.crypto.SecretKey;
+import java.nio.charset.StandardCharsets;
+import java.util.Date;
+import java.util.function.Function;
+
+@Service
+public class JwtService {
+
+    @Value("${maka.jwt.secret}")
+    private String secretKeyString;
+
+    @Value("${maka.jwt.expiration}")
+    private long jwtExpirationInMs;
+
+    private SecretKey getSigningKey() {
+        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
+        return Keys.hmacShaKeyFor(keyBytes);
+    }
+
+    public String generateToken(String email, String role) {
+        return Jwts.builder()
+                .subject(email)
+                .claim("role", role)
+                .issuedAt(new Date(System.currentTimeMillis()))
+                .expiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
+                .signWith(getSigningKey())
+                .compact();
+    }
+
+    public String extractEmail(String token) {
+        return extractClaim(token, Claims::getSubject);
+    }
+
+    public String extractRole(String token) {
+        return extractClaim(token, claims -> claims.get("role", String.class));
+    }
+
+    public boolean isTokenValid(String token) {
+        try {
+            Date expiration = extractClaim(token, Claims::getExpiration);
+            return !expiration.before(new Date());
+        } catch (Exception e) {
+            return false;
+        }
+    }
+
+    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
+        final Claims claims = extractAllClaims(token);
+        return claimsResolver.apply(claims);
+    }
+
+    private Claims extractAllClaims(String token) {
+        return Jwts.parser()
+                .verifyWith(getSigningKey())
+                .build()
+                .parseSignedClaims(token)
+                .getPayload();
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/JwtServiceTest.java b/backend/src/main/java/com/maka/api/auth/JwtServiceTest.java
new file mode 100644
index 0000000..a407682
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/JwtServiceTest.java
@@ -0,0 +1,34 @@
+package com.maka.api.auth;
+
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Test;
+import org.springframework.test.util.ReflectionTestUtils;
+import static org.junit.jupiter.api.Assertions.*;
+
+public class JwtServiceTest {
+
+    private JwtService jwtService;
+
+    @BeforeEach
+    void setUp() {
+        jwtService = new JwtService();
+        ReflectionTestUtils.setField(jwtService, "secretKeyString", "minhaChaveSecretaSuperSuperSeguraParaOTrabalhoDeTI2026DevesTerPeloMenos256BitsDeComprimento!");
+        ReflectionTestUtils.setField(jwtService, "jwtExpirationInMs", 36000000L);
+    }
+
+    @Test
+    void deveGerarETokenValido() {
+        String token = jwtService.generateToken("teste@maka.com", "ROLE_MANAGER");
+        assertNotNull(token);
+        assertTrue(jwtService.isTokenValid(token));
+        assertEquals("teste@maka.com", jwtService.extractEmail(token));
+        assertEquals("ROLE_MANAGER", jwtService.extractRole(token));
+    }
+
+    @Test
+    void deveRetornarInvalidoParaTokenModificado() {
+        String token = jwtService.generateToken("teste@maka.com", "ROLE_MANAGER");
+        String tokenModificado = token + "erro";
+        assertFalse(jwtService.isTokenValid(tokenModificado));
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/dto/LoginRequest.java b/backend/src/main/java/com/maka/api/auth/dto/LoginRequest.java
new file mode 100644
index 0000000..961effa
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/dto/LoginRequest.java
@@ -0,0 +1,11 @@
+package com.maka.api.auth.dto;
+
+public class LoginRequest {
+    private String email;
+    private String password;
+
+    public String getEmail() { return email; }
+    public void setEmail(String email) { this.email = email; }
+    public String getPassword() { return password; }
+    public void setPassword(String password) { this.password = password; }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/dto/LoginResponse.java b/backend/src/main/java/com/maka/api/auth/dto/LoginResponse.java
new file mode 100644
index 0000000..167457d
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/dto/LoginResponse.java
@@ -0,0 +1,35 @@
+package com.maka.api.auth.dto;
+
+public class LoginResponse {
+    private String token;
+    private UserDto user;
+
+    public LoginResponse(String token, UserDto user) {
+        this.token = token;
+        this.user = user;
+    }
+
+    public String getToken() { return token; }
+    public void setToken(String token) { this.token = token; }
+    public UserDto getUser() { return user; }
+    public void setUser(UserDto user) { this.user = user; }
+
+    public static class UserDto {
+        private String uid;
+        private String email;
+        private String role;
+        private String username;
+
+        public UserDto(String uid, String email, String role, String username) {
+            this.uid = uid;
+            this.email = email;
+            this.role = role;
+            this.username = username;
+        }
+
+        public String getUid() { return uid; }
+        public String getEmail() { return email; }
+        public String getRole() { return role; }
+        public String getUsername() { return username; }
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/auth/dto/UserResponse.java b/backend/src/main/java/com/maka/api/auth/dto/UserResponse.java
new file mode 100644
index 0000000..92b943d
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/auth/dto/UserResponse.java
@@ -0,0 +1,13 @@
+package com.maka.api.auth.dto;
+
+public class UserResponse {
+    private String email;
+    private String role;
+
+    public UserResponse(String email, String role) {
+        this.email = email;
+        this.role = role.replace("ROLE_", "").toLowerCase();
+    }
+    public String getEmail() { return email; }
+    public String getRole() { return role; }
+}
diff --git a/backend/src/main/java/com/maka/api/config/JwtAuthenticationEntryPoint.java b/backend/src/main/java/com/maka/api/config/JwtAuthenticationEntryPoint.java
new file mode 100644
index 0000000..e4b3a75
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/config/JwtAuthenticationEntryPoint.java
@@ -0,0 +1,16 @@
+package com.maka.api.config;
+
+import jakarta.servlet.http.HttpServletRequest;
+import jakarta.servlet.http.HttpServletResponse;
+import org.springframework.security.core.AuthenticationException;
+import org.springframework.security.web.AuthenticationEntryPoint;
+import org.springframework.stereotype.Component;
+import java.io.IOException;
+
+@Component
+public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
+    @Override
+    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
+        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autorizado.");
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/config/JwtFilter.java b/backend/src/main/java/com/maka/api/config/JwtFilter.java
new file mode 100644
index 0000000..d848893
--- /dev/null
+++ b/backend/src/main/java/com/maka/api/config/JwtFilter.java
@@ -0,0 +1,35 @@
+package com.maka.api.config;
+
+import com.maka.api.auth.JwtService;
+import jakarta.servlet.FilterChain;
+import jakarta.servlet.ServletException;
+import jakarta.servlet.http.HttpServletRequest;
+import jakarta.servlet.http.HttpServletResponse;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
+import org.springframework.security.core.authority.SimpleGrantedAuthority;
+import org.springframework.security.core.context.SecurityContextHolder;
+import org.springframework.stereotype.Component;
+import org.springframework.web.filter.OncePerRequestFilter;
+import java.io.IOException;
+import java.util.Collections;
+
+@Component
+public class JwtFilter extends OncePerRequestFilter {
+    @Autowired private JwtService jwtService;
+
+    @Override
+    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
+        String authHeader = request.getHeader("Authorization");
+        if (authHeader != null && authHeader.startsWith("Bearer ")) {
+            String token = authHeader.substring(7);
+            if (jwtService.isTokenValid(token)) {
+                String email = jwtService.extractEmail(token);
+                String role = jwtService.extractRole(token);
+                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
+                SecurityContextHolder.getContext().setAuthentication(auth);
+            }
+        }
+        filterChain.doFilter(request, response);
+    }
+}
diff --git a/backend/src/main/java/com/maka/api/config/SecurityConfig.java b/backend/src/main/java/com/maka/api/config/SecurityConfig.java
index 7fc16b9..59ed48d 100644
--- a/backend/src/main/java/com/maka/api/config/SecurityConfig.java
+++ b/backend/src/main/java/com/maka/api/config/SecurityConfig.java
@@ -1,7 +1,7 @@
 package com.maka.api.config;
 
 import java.util.List;
-
+import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
@@ -13,6 +13,7 @@ import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.web.SecurityFilterChain;
+import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 import org.springframework.web.cors.CorsConfiguration;
 import org.springframework.web.cors.CorsConfigurationSource;
 import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
@@ -20,9 +21,15 @@ import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 @Configuration
 public class SecurityConfig {
 
-    @Value("${maka.cors.allowed-origins}")
+    @Value("${maka.cors.allowed-origins:http://localhost:4200}")
     private List<String> allowedOrigins;
 
+    @Autowired
+    private JwtFilter jwtFilter;
+
+    @Autowired
+    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
+
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
@@ -30,14 +37,23 @@ public class SecurityConfig {
                 .cors(Customizer.withDefaults())
                 .formLogin(AbstractHttpConfigurer::disable)
                 .httpBasic(AbstractHttpConfigurer::disable)
+                .exceptionHandling(eh -> eh.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                 .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .authorizeHttpRequests(auth -> auth
+                        // Rotas inteiramente Públicas
                         .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
-                        .requestMatchers("/api/auth/**").permitAll()
+                        .requestMatchers("/api/auth/login").permitAll()
                         .requestMatchers(HttpMethod.GET, "/api/cardapio/**").permitAll()
                         .requestMatchers(HttpMethod.POST, "/api/pedidos").permitAll()
-                        // TODO (Auth): trocar por .authenticated() quando o JWT estiver pronto
-                        .anyRequest().permitAll());
+                        
+                        // Permissões por Perfil (Exigência do Gabriel)
+                        .requestMatchers("/api/admin/**", "/api/funcionarios/**").hasRole("MANAGER")
+                        .requestMatchers("/api/cozinha/**").hasAnyRole("MANAGER", "KITCHEN")
+                        .requestMatchers("/api/entregas/**").hasAnyRole("MANAGER", "DELIVERY")
+                        
+                        // Qualquer outra rota exige apenas estar autenticado via JWT
+                        .anyRequest().authenticated())
+                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
         return http.build();
     }
 
@@ -52,9 +68,8 @@ public class SecurityConfig {
         config.setAllowedOrigins(allowedOrigins);
         config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
         config.setAllowedHeaders(List.of("*"));
-
         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", config);
         return source;
     }
-}
\ No newline at end of file
+}
diff --git a/backend/src/main/resources/application.properties b/backend/src/main/resources/application.properties
index 5456851..f98f217 100644
--- a/backend/src/main/resources/application.properties
+++ b/backend/src/main/resources/application.properties
@@ -13,9 +13,9 @@ spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
 spring.jpa.show-sql=true
 spring.sql.init.mode=never
 
-# JWT (troquem o segredo depois; precisa ter 32+ caracteres)
-maka.jwt.secret=${JWT_SECRET:maka-segredo-de-desenvolvimento-trocar-depois}
-maka.jwt.expiration-ms=86400000
+# JWT (precisa ter 32+ caracteres; em produção use a variável JWT_SECRET)
+maka.jwt.secret=${JWT_SECRET:minhaChaveSecretaSuperSuperSeguraParaOTrabalhoDeTI2026DevesTerPeloMenos256BitsDeComprimento!}
+maka.jwt.expiration=36000000
 
 # Frontend Angular
-maka.cors.allowed-origins=http://localhost:4200
\ No newline at end of file
+maka.cors.allowed-origins=http://localhost:4200,http://localhost:3000
\ No newline at end of file
-- 
2.43.0

