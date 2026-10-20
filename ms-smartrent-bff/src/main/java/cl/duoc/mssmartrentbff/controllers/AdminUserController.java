package cl.duoc.mssmartrentbff.controllers;

import cl.duoc.mssmartrentbff.dto.GraphUserDto;
import cl.duoc.mssmartrentbff.services.AzureGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminUserController {

    private final AzureGraphService azureGraphService;

    public AdminUserController(AzureGraphService azureGraphService) {
        this.azureGraphService = azureGraphService;
    }

    @GetMapping({"/api/users", "/v1/api/users", "/v2/api/users", "/users"})
    public ResponseEntity<List<GraphUserDto>> getTenantUsers() {
        List<GraphUserDto> users = azureGraphService.getAllTenantUsers();
        return ResponseEntity.ok(users);
    }
}
