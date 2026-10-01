package cl.duoc.mssmartrentbff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;

@Configuration
@SuppressWarnings("null")
public class GatewayConfig {

    private static final String CATALOG_URL = "http://smartrent-catalog:8081";
    private static final String RENTALS_URL = "http://smartrent-rentals:8082";
    private static final String REPORT_URL = "http://smartrent-report:8083";
    private static final String AUDIT_URL = "http://smartrent-audit:8084";
    private static final String CART_URL = "http://smartrent-cart:8085";
    private static final String CHECKOUT_URL = "http://smartrent-checkout:8086";

    @Bean
    public RouterFunction<ServerResponse> catalogRoute() {
        return route("catalog_route")
                .route(path("/api/catalog/**"), http(CATALOG_URL))
                .build()
                .and(route("catalog_route_v1")
                        .route(path("/v1/api/catalog/**"), http(CATALOG_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("catalog_route_v2")
                        .route(path("/v2/api/catalog/**"), http(CATALOG_URL))
                        .before(stripPrefix(1))
                        .build());
    }

    @Bean
    public RouterFunction<ServerResponse> rentalsRoute() {
        return route("rentals_route")
                .route(path("/api/rentals/**"), http(RENTALS_URL))
                .build()
                .and(route("rentals_route_v1")
                        .route(path("/v1/api/rentals/**"), http(RENTALS_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("rentals_route_v2")
                        .route(path("/v2/api/rentals/**"), http(RENTALS_URL))
                        .before(stripPrefix(1))
                        .build());
    }

    @Bean
    public RouterFunction<ServerResponse> reportsRoute() {
        return route("reports_route")
                .route(path("/api/reports/**"), http(REPORT_URL))
                .build()
                .and(route("reports_route_v1")
                        .route(path("/v1/api/reports/**"), http(REPORT_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("reports_route_v2")
                        .route(path("/v2/api/reports/**"), http(REPORT_URL))
                        .before(stripPrefix(1))
                        .build());
    }

    @Bean
    public RouterFunction<ServerResponse> auditRoute() {
        return route("audit_route")
                .route(path("/api/audit/**"), http(AUDIT_URL))
                .build()
                .and(route("audit_route_v1")
                        .route(path("/v1/api/audit/**"), http(AUDIT_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("audit_route_v2")
                        .route(path("/v2/api/audit/**"), http(AUDIT_URL))
                        .before(stripPrefix(1))
                        .build());
    }

    @Bean
    public RouterFunction<ServerResponse> cartRoute() {
        return route("cart_route")
                .route(path("/api/cart/**"), http(CART_URL))
                .build()
                .and(route("cart_route_v1")
                        .route(path("/v1/api/cart/**"), http(CART_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("cart_route_v2")
                        .route(path("/v2/api/cart/**"), http(CART_URL))
                        .before(stripPrefix(1))
                        .build());
    }

    @Bean
    public RouterFunction<ServerResponse> checkoutRoute() {
        return route("checkout_route")
                .route(path("/api/checkout/**"), http(CHECKOUT_URL))
                .build()
                .and(route("checkout_route_v1")
                        .route(path("/v1/api/checkout/**"), http(CHECKOUT_URL))
                        .before(stripPrefix(1))
                        .build())
                .and(route("checkout_route_v2")
                        .route(path("/v2/api/checkout/**"), http(CHECKOUT_URL))
                        .before(stripPrefix(1))
                        .build());
    }
}
