package cl.duoc.mssmartrentbff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ADMIN_ROLE_NAME = "Administrador";
    private static final String ROLE_AUDITOR = "ROLE_AUDITOR";
    private static final String AUDITOR_ROLE_NAME = "Auditor";

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:https://753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com/753ae9df-d5ec-4c6b-8227-b334fc775087/discovery/v2.0/keys}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://753ae9df-d5ec-4c6b-8227-b334fc775087.ciamlogin.com/753ae9df-d5ec-4c6b-8227-b334fc775087/v2.0}")
    private String issuerUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Object>(
                "aud",
                aud -> {
                    if (aud == null) return false;
                    String audStr = aud.toString();
                    return audStr.contains("749dc676-d557-460a-b138-dac9a6744b6e");
                }
        );

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter conv) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/catalog/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/catalog/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/catalog/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/users/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/users/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/users/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/users/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/admin/**"))
                .permitAll()
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/reports/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/reports/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/reports/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/reports/**"))
                .hasAnyAuthority(ROLE_ADMIN, ADMIN_ROLE_NAME)
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/audit/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/audit/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/audit/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/audit/**"))
                .hasAnyAuthority(ROLE_AUDITOR, AUDITOR_ROLE_NAME, ROLE_ADMIN, ADMIN_ROLE_NAME)
                .anyRequest().authenticated());
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(conv)));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var autoridades = new ArrayList<GrantedAuthority>(scopes.convert(jwt));
            var roles = jwt.getClaimAsStringList("roles");

            if (roles != null) {
                roles.forEach(r -> {
                    autoridades.add(new SimpleGrantedAuthority(r));
                    if (ADMIN_ROLE_NAME.equalsIgnoreCase(r) || ROLE_ADMIN.equalsIgnoreCase(r)) {
                        autoridades.add(new SimpleGrantedAuthority(ROLE_ADMIN));
                        autoridades.add(new SimpleGrantedAuthority(ADMIN_ROLE_NAME));
                    }
                    if (AUDITOR_ROLE_NAME.equalsIgnoreCase(r) || ROLE_AUDITOR.equalsIgnoreCase(r)) {
                        autoridades.add(new SimpleGrantedAuthority(ROLE_AUDITOR));
                        autoridades.add(new SimpleGrantedAuthority(AUDITOR_ROLE_NAME));
                    }
                });
            }
            return autoridades;
        });

        return converter;
    }

    @SuppressWarnings("java:S5122")
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:*",
                "https://smartrent-portal.s3.us-east-1.amazonaws.com",
                "*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Authorization", "Content-Type", "WWW-Authenticate"));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
