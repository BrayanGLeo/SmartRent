package cl.duoc.mssmartrentbff.services;

import cl.duoc.mssmartrentbff.config.AzureGraphProperties;
import cl.duoc.mssmartrentbff.controllers.AdminUserController;
import cl.duoc.mssmartrentbff.dto.GraphUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class AzureGraphServiceTest {

    private AzureGraphProperties properties;
    private AzureGraphService service;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        properties = new AzureGraphProperties();
        properties.setTenantId("tenant-1");
        properties.setClientId("client-1");
        properties.setClientSecret("secret-1");
        service = new AzureGraphService(properties);
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void graphUserDtoAndAdminController_ShouldWorkCorrectly() {
        GraphUserDto emptyDto = new GraphUserDto();
        emptyDto.setId("1");
        emptyDto.setFullName("Nombre");
        emptyDto.setEmail("mail@test.com");
        emptyDto.setTenantId("t1");
        emptyDto.setCompany("Comp");
        emptyDto.setRole("ADMIN");
        emptyDto.setStatus("ACTIVO");
        emptyDto.setCreatedAt("2026-01-01");

        assertEquals("1", emptyDto.getId());
        assertEquals("Nombre", emptyDto.getFullName());
        assertEquals("mail@test.com", emptyDto.getEmail());
        assertEquals("t1", emptyDto.getTenantId());
        assertEquals("Comp", emptyDto.getCompany());
        assertEquals("ADMIN", emptyDto.getRole());
        assertEquals("ACTIVO", emptyDto.getStatus());
        assertEquals("2026-01-01", emptyDto.getCreatedAt());

        AzureGraphService mockService = mock(AzureGraphService.class);
        when(mockService.getAllTenantUsers()).thenReturn(List.of(emptyDto));
        AdminUserController controller = new AdminUserController(mockService);

        ResponseEntity<List<GraphUserDto>> response = controller.getTenantUsers();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }

    @Test
    void getGraphAccessToken_WithoutSecret_ThrowsException() {
        properties.setClientSecret(null);
        assertThrows(IllegalStateException.class, () -> service.getGraphAccessToken());

        properties.setClientSecret("   ");
        assertThrows(IllegalStateException.class, () -> service.getGraphAccessToken());
    }

    @Test
    void getGraphAccessToken_CachesTokenAndHandlesErrors() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"tok-123\",\"expires_in\":3600}"));

        String firstToken = service.getGraphAccessToken();
        String secondToken = service.getGraphAccessToken();

        assertEquals("tok-123", firstToken);
        assertEquals("tok-123", secondToken);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

        // Reset cache to test missing token and exception branches
        ReflectionTestUtils.setField(service, "cachedAccessToken", null);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"error\":\"no_token\"}"));
        assertThrows(IllegalStateException.class, () -> service.getGraphAccessToken());

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));
        assertThrows(IllegalStateException.class, () -> service.getGraphAccessToken());
    }

    @Test
    void getAllTenantUsers_ParsesGraphResponseAndFallsBackOnError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"tok-graph\",\"expires_in\":3600}"));

        String graphJson = """
                {
                  "value": [
                    {"id":"u1","displayName":"Brayan Admin","mail":"admin@test.cl","userPrincipalName":"upn1","accountEnabled":true},
                    {"id":"u2","displayName":"Lucas Bodega","mail":null,"userPrincipalName":"bodega@test.cl","accountEnabled":true},
                    {"id":"u3","displayName":"Diego Auditor","mail":"   ","userPrincipalName":"auditor@test.cl","accountEnabled":false},
                    {"id":"u4","displayName":"Cliente Final","mail":"cliente@test.cl","userPrincipalName":"cliente@test.cl","accountEnabled":true}
                  ]
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(graphJson));

        List<GraphUserDto> users = service.getAllTenantUsers();
        assertEquals(4, users.size());
        assertEquals("ADMIN", users.get(0).getRole());
        assertEquals("OPERADOR", users.get(1).getRole());
        assertEquals("bodega@test.cl", users.get(1).getEmail());
        assertEquals("AUDITOR", users.get(2).getRole());
        assertEquals("INACTIVO", users.get(2).getStatus());
        assertEquals("CLIENTE", users.get(3).getRole());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Graph unavailable"));

        List<GraphUserDto> fallback = service.getAllTenantUsers();
        assertEquals(5, fallback.size());
    }
}
