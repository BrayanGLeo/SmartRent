package cl.duoc.mssmartrentbff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter conv) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/catalog/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/catalog/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/catalog/**"))
                .permitAll()
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/reports/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/reports/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/reports/**"))
                .hasAnyAuthority("ROLE_ADMIN", "Administrador")
                .requestMatchers(
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/api/audit/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v1/api/audit/**"),
                        org.springframework.security.web.util.matcher.AntPathRequestMatcher
                                .antMatcher("/v2/api/audit/**"))
                .hasAnyAuthority("ROLE_AUDITOR", "Auditor", "ROLE_ADMIN", "Administrador")
                .anyRequest().authenticated());
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(conv)));
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
                    if ("Administrador".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)) {
                        autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        autoridades.add(new SimpleGrantedAuthority("Administrador"));
                    }
                    if ("Auditor".equalsIgnoreCase(r) || "ROLE_AUDITOR".equalsIgnoreCase(r)) {
                        autoridades.add(new SimpleGrantedAuthority("ROLE_AUDITOR"));
                        autoridades.add(new SimpleGrantedAuthority("Auditor"));
                    }
                });
            }
            return autoridades;
        });

        return converter;
    }

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
