package cl.duoc.mssmartrentbff.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayConfigTest {

    private final GatewayConfig config = new GatewayConfig();

    @Test
    void catalogRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.catalogRoute();
        assertNotNull(route);
    }

    @Test
    void rentalsRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.rentalsRoute();
        assertNotNull(route);
    }

    @Test
    void reportsRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.reportsRoute();
        assertNotNull(route);
    }

    @Test
    void auditRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.auditRoute();
        assertNotNull(route);
    }

    @Test
    void cartRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.cartRoute();
        assertNotNull(route);
    }

    @Test
    void checkoutRoute_createsRouterFunction() {
        RouterFunction<ServerResponse> route = config.checkoutRoute();
        assertNotNull(route);
    }
}
