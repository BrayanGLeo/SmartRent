package cl.duoc.mssmartrentbff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
@SuppressWarnings("null")
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> catalogRoute() {
        return route("catalog_route")
                .route(path("/api/catalog/**"), http("http://smartrent-catalog:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> rentalsRoute() {
        return route("rentals_route")
                .route(path("/api/rentals/**"), http("http://smartrent-rentals:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> reportsRoute() {
        return route("reports_route")
                .route(path("/api/reports/**"), http("http://smartrent-report:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> auditRoute() {
        return route("audit_route")
                .route(path("/api/audit/**"), http("http://smartrent-audit:8084"))
                .build();
    }
}
