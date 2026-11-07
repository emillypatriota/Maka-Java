package com.maka.api.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Regras de acesso do sistema inteiro num lugar só.
 *
 * Perfis (authority = ROLE_ + nome do enum Perfil): ADMIN, MANAGER (gerente), KITCHEN (cozinha),
 * DELIVERY (entregador), CLIENTE.
 * ADMIN passa em tudo que o MANAGER passa; MANAGER também entra nas áreas de cozinha e entrega
 * (o front do gerente tem as telas /gerente/cozinha e /gerente/entregas).
 *
 * Rota nova em /api/admin/** já nasce protegida (MANAGER/ADMIN). Rota fora da lista de
 * baixo exige apenas estar logado. @PreAuthorize nos controllers é uma segunda trava
 * opcional: se usar, lembre de incluir 'ADMIN' (ex.: hasAnyRole('ADMIN','MANAGER')).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Grupos de perfis reutilizados nas regras abaixo
    private static final String[] GERENCIA = {"ADMIN", "MANAGER"};
    private static final String[] ENTREGA = {"ADMIN", "MANAGER", "DELIVERY"};
    private static final String[] PAINEL_PEDIDOS = {"ADMIN", "MANAGER", "KITCHEN", "DELIVERY"};
    private static final String[] FAZ_PEDIDO = {"ADMIN", "MANAGER", "CLIENTE"};

    @Value("${maka.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 401 = não logado (entry point) | 403 = logado sem permissão (access denied handler)
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // A ORDEM IMPORTA: a primeira regra que casar vale. Específico antes do genérico.
                .authorizeHttpRequests(auth -> auth
                        // ── Públicas ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cardapio", "/api/cardapio/**").permitAll()

                        // ── Qualquer perfil logado ──────────────────────────────────
                        .requestMatchers("/api/auth/me").authenticated()
                        // (o Service do pedido deve conferir que o cliente só vê o PRÓPRIO pedido)
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/*/status").authenticated()

                        // ── Cliente: faz pedido e vê o próprio histórico ────────────
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole(FAZ_PEDIDO)
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/meus").hasRole("CLIENTE")

                        // ── Painéis da cozinha e do entregador ──────────────────────
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/entregador").hasAnyRole(ENTREGA)
                        .requestMatchers("/api/pedidos", "/api/pedidos/**", "/api/admin/pedidos", "/api/admin/pedidos/**")
                                .hasAnyRole(PAINEL_PEDIDOS)

                        // ── Gerência ────────────────────────────────────────────────
                        .requestMatchers("/api/cardapio/**").hasAnyRole(GERENCIA)      // POST/PUT/PATCH/DELETE
                        .requestMatchers("/api/funcionarios", "/api/funcionarios/**").hasAnyRole(GERENCIA)
                        .requestMatchers("/api/insights/**").hasAnyRole(GERENCIA)
                        // Só o ADMIN cria usuários internos (gerente, cozinheiro...) — RF-041
                        .requestMatchers("/api/admin/usuarios", "/api/admin/usuarios/**").hasRole("ADMIN")
                        // Todo o resto de /api/admin (pratos, ingredientes, fornecedores, estoque, dashboard...)
                        .requestMatchers("/api/admin/**").hasAnyRole(GERENCIA)

                        // ── Padrão: precisa estar logado ────────────────────────────
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
