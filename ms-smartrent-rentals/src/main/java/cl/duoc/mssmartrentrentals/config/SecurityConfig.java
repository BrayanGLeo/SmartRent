package cl.duoc.mssmartrentrentals.config;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@SuppressWarnings("java:S5122")
public class SecurityConfig {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ADMIN_ROLE_NAME = "Administrador";
    private static final String ROLE_JEFEBODEGA = "ROLE_JEFEBODEGA";
    private static final String JEFEBODEGA_ROLE_NAME = "JefeBodega";

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
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(conv)));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> autoridades = new LinkedHashSet<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles != null) {
                roles.forEach(r -> autoridades.addAll(mapRole(r)));
            }
            return new ArrayList<>(autoridades);
        });

        return converter;
    }

    static List<GrantedAuthority> mapRole(String rawRole) {
        List<GrantedAuthority> result = new ArrayList<>();
        if (rawRole == null || rawRole.isBlank()) {
            return result;
        }
        String role = rawRole.trim();
        result.add(new SimpleGrantedAuthority(role));

        String normalized = role.toUpperCase(Locale.ROOT);
        if (normalized.startsWith(ROLE_PREFIX)) {
            normalized = normalized.substring(ROLE_PREFIX.length());
        }

        switch (normalized) {
            case "ADMIN", "ADMINISTRADOR" -> {
                result.add(new SimpleGrantedAuthority(ROLE_ADMIN));
                result.add(new SimpleGrantedAuthority(ADMIN_ROLE_NAME));
            }
            case "JEFEBODEGA", "JEFE_BODEGA", "JEFE BODEGA" -> {
                result.add(new SimpleGrantedAuthority(ROLE_JEFEBODEGA));
                result.add(new SimpleGrantedAuthority(JEFEBODEGA_ROLE_NAME));
            }
            default -> result.add(new SimpleGrantedAuthority(ROLE_PREFIX + normalized));
        }
        return result;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
