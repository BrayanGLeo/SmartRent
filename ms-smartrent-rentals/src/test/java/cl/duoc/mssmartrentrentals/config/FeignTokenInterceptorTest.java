package cl.duoc.mssmartrentrentals.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.mockito.Mockito.*;

class FeignTokenInterceptorTest {

    @Test
    void apply_ShouldAddAuthorizationHeaderWhenAuthenticated() {
        FeignTokenInterceptor interceptor = new FeignTokenInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        SecurityContext securityContext = mock(SecurityContext.class);
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "user").build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        interceptor.apply(template);

        verify(template).header("Authorization", "Bearer test-token");
    }

    @Test
    void apply_ShouldNotAddHeaderWhenNotAuthenticated() {
        FeignTokenInterceptor interceptor = new FeignTokenInterceptor();
        RequestTemplate template = mock(RequestTemplate.class);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        interceptor.apply(template);

        verify(template, never()).header(anyString(), anyString());
    }
}
