package thitkho.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "public")
public class PublicEndpointConfig {

    private List<String> endpoints;
    public List<String> getEndpoints() {
        return endpoints;
    }
    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }
}