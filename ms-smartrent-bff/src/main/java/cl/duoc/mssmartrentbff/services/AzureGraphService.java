package cl.duoc.mssmartrentbff.services;

import cl.duoc.mssmartrentbff.config.AzureGraphProperties;
import cl.duoc.mssmartrentbff.dto.GraphUserDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AzureGraphService {

    private static final Logger log = LoggerFactory.getLogger(AzureGraphService.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERADOR = "OPERADOR";
    private static final String ROLE_AUDITOR = "AUDITOR";
    private static final String ROLE_CLIENTE = "CLIENTE";
    private static final String STATUS_ACTIVO = "ACTIVO";
    private static final String STATUS_INACTIVO = "INACTIVO";
    private static final String DEFAULT_CREATED_AT = "2024-01-15";
    private static final String COMPANY_CIAM = "SmartRent Red CIAM";
    private static final String COMPANY_CIAM_DOMAIN = "SmartRent Red CIAM (pepito123.onmicrosoft.com)";

    private final AzureGraphProperties azureGraphProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AzureGraphService(AzureGraphProperties azureGraphProperties) {
        this.azureGraphProperties = azureGraphProperties;
    }

    /**
     * Obtiene el Access Token de Microsoft Graph mediante Client Credentials Grant
     */
    public String getGraphAccessToken() {
        if (azureGraphProperties.getClientSecret() == null || azureGraphProperties.getClientSecret().isBlank()) {
            throw new IllegalStateException("azure.graph.client-secret no configurado (defina AZURE_GRAPH_CLIENT_SECRET)");
        }
        String tokenUrl = "https://login.microsoftonline.com/" + azureGraphProperties.getTenantId() + "/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", azureGraphProperties.getClientId());
        body.add("client_secret", azureGraphProperties.getClientSecret());
        body.add("scope", "https://graph.microsoft.com/.default");
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("access_token")) {
                return root.get("access_token").asText();
            }
            throw new IllegalStateException("Respuesta de Azure no contiene access_token");
        } catch (Exception e) {
            log.error("Fallo al autenticar con Microsoft Entra ID: {}", e.getMessage());
            throw new IllegalStateException("Error autenticando con Azure IDaaS", e);
        }
    }

    /**
     * Consulta a Microsoft Graph API para obtener todos los usuarios del tenant
     */
    public List<GraphUserDto> getAllTenantUsers() {
        List<GraphUserDto> resultList = new ArrayList<>();
        try {
            String token = Objects.requireNonNull(getGraphAccessToken());
            String graphUrl = "https://graph.microsoft.com/v1.0/users?$select=id,displayName,userPrincipalName,mail,accountEnabled";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(Objects.requireNonNull(List.of(MediaType.APPLICATION_JSON)));

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    graphUrl,
                    Objects.requireNonNull(HttpMethod.GET),
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode valueArray = root.get("value");

            if (valueArray != null && valueArray.isArray()) {
                for (JsonNode userNode : valueArray) {
                    resultList.add(mapUserNode(userNode));
                }
            }
            log.info("Microsoft Graph API: Se cargaron {} usuarios reales del tenant", resultList.size());
            return resultList;
        } catch (Exception e) {
            log.error("Error al consultar usuarios en Microsoft Graph: {}", e.getMessage());
            return getFallbackUsers();
        }
    }

    private GraphUserDto mapUserNode(JsonNode userNode) {
        String id = userNode.path("id").asText();
        String displayName = userNode.path("displayName").asText("Usuario Tenant");
        String email = resolveEmail(userNode);
        boolean isEnabled = userNode.path("accountEnabled").asBoolean(true);
        String role = inferRole(displayName, email);

        return new GraphUserDto(
                id,
                displayName,
                email,
                azureGraphProperties.getTenantId(),
                COMPANY_CIAM_DOMAIN,
                role,
                isEnabled ? STATUS_ACTIVO : STATUS_INACTIVO,
                DEFAULT_CREATED_AT
        );
    }

    private static String resolveEmail(JsonNode userNode) {
        String email = userNode.path("mail").asText();
        if (email == null || email.isBlank() || "null".equals(email)) {
            return userNode.path("userPrincipalName").asText();
        }
        return email;
    }

    private static String inferRole(String displayName, String email) {
        String nameLower = (displayName + " " + email).toLowerCase(Locale.ROOT);
        if (nameLower.contains("admin") || nameLower.contains("brayan")) {
            return ROLE_ADMIN;
        }
        if (nameLower.contains("bodega") || nameLower.contains("operador")) {
            return ROLE_OPERADOR;
        }
        if (nameLower.contains("auditor")) {
            return ROLE_AUDITOR;
        }
        return ROLE_CLIENTE;
    }

    private List<GraphUserDto> getFallbackUsers() {
        String tid = azureGraphProperties.getTenantId();
        return List.of(
            new GraphUserDto("42392204-fa6d-42ec-8118-542a929e9f82", "Brayan", "Brayan@pepito123.onmicrosoft.com", tid, COMPANY_CIAM, ROLE_ADMIN, STATUS_ACTIVO, DEFAULT_CREATED_AT),
            new GraphUserDto("6f3d972f-78d8-40a5-a916-023271ed94cf", "Juan Administrador", "Juan@pepito123.onmicrosoft.com", tid, COMPANY_CIAM, ROLE_ADMIN, STATUS_ACTIVO, DEFAULT_CREATED_AT),
            new GraphUserDto("79c25ca9-bc8e-4709-b345-6b19b5ae6e1f", "Lucas Jefe de Bodega", "Lucas@pepito123.onmicrosoft.com", tid, COMPANY_CIAM, ROLE_OPERADOR, STATUS_ACTIVO, "2024-02-01"),
            new GraphUserDto("b4fc592c-d2d9-4aab-aead-565a4df1afc3", "Diego Auditor", "Diego@pepito123.onmicrosoft.com", tid, COMPANY_CIAM, ROLE_AUDITOR, STATUS_ACTIVO, "2024-01-20"),
            new GraphUserDto("b89795b0-c4c2-4b55-b525-e80607ea1edb", "DANIEL GODOY SEPULVEDA", "dan.godoys_duocuc.cl#EXT#@pepito123.onmicrosoft.com", tid, COMPANY_CIAM, ROLE_CLIENTE, STATUS_ACTIVO, "2024-02-18")
        );
    }
}
