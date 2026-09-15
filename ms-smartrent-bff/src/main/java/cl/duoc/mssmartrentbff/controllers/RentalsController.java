package cl.duoc.mssmartrentbff.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RentalsController {

    private static final Logger log = LoggerFactory.getLogger(RentalsController.class);
    private static final String KEY_ID = "id";
    private static final String KEY_MAQUINA = "maquina";
    private static final String KEY_ESTADO = "estado";

    @GetMapping("/rentals")
    @PreAuthorize("hasAuthority('SCOPE_Arriendos.Leer')")
    public List<Map<String, String>> listar(@AuthenticationPrincipal Jwt jwt) {
        if (log.isInfoEnabled()) {
            log.info("Sirviendo arriendos a usuario: {}", jwt.getClaimAsString("sub"));
            log.info("Roles detectados: {}", jwt.getClaimAsStringList("roles"));
        }
        
        return List.of(
            Map.of(KEY_ID, "RNT-1042", KEY_MAQUINA, "Excavadora CAT 320", KEY_ESTADO, "EN_TERRENO"),
            Map.of(KEY_ID, "RNT-1043", KEY_MAQUINA, "Generador 50kVA", KEY_ESTADO, "APROBADO"),
            Map.of(KEY_ID, "RNT-1047", KEY_MAQUINA, "Martillo Demoledor", KEY_ESTADO, "SOLICITADO")
        );
    }

    @PutMapping("/rentals/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_Arriendos.Escribir') and hasAnyRole('Admin', 'JefeBodega')")
    public Map<String, String> cambiarEstado(@PathVariable String id, @RequestBody Map<String, String> body) {
        return Map.of(KEY_ID, id, KEY_ESTADO, body.get(KEY_ESTADO), "mensaje", "Estado actualizado correctamente");
    }

    @GetMapping("/reportes/kpis")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> kpis() {
        return Map.of("arriendosActivos", 12, "equiposEnTerreno", 5, "tiempoCicloPromedioDias", 3.4);
    }
}
