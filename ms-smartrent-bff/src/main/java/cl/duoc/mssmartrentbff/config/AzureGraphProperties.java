package cl.duoc.mssmartrentbff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.graph")
public class AzureGraphProperties {

    private String tenantId = "753ae9df-d5ec-4c6b-8227-b334fc775087";
    private String clientId = "749dc676-d557-460a-b138-dac9a6744b6e";
    private String clientSecret;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
