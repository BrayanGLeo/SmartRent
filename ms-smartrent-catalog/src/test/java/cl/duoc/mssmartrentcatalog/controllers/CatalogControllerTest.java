package cl.duoc.mssmartrentcatalog.controllers;

import cl.duoc.mssmartrentcatalog.controllers.CatalogController.MachineRequest;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.services.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CatalogController catalogController;

    private Machine testMachine;

    @BeforeEach
    void setUp() {
        testMachine = new Machine();
        testMachine.setId(1L);
        testMachine.setName("Grúa");
    }

    @Test
    void getCatalog_ShouldReturnAvailableMachines() {
        when(catalogService.getAllAvailableMachines()).thenReturn(List.of(testMachine));

        ResponseEntity<List<Machine>> response = catalogController.getCatalog();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Machine> body = Objects.requireNonNull(response.getBody());
        assertFalse(body.isEmpty());
        assertEquals("Grúa", body.get(0).getName());
    }

    @Test
    void createMachine_ShouldMapRequestAndReturnCreatedMachine() {
        MachineRequest request = new MachineRequest("Grúa", "SN123", 1L, 50000.0, true);
        when(catalogService.createMachine(any(Machine.class))).thenReturn(testMachine);

        ResponseEntity<Machine> response = catalogController.createMachine(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(catalogService, times(1)).createMachine(any(Machine.class));
    }

    @Test
    void updateAvailability_ShouldReturnUpdatedMachine() {
        when(catalogService.updateMachineAvailability(1L, false)).thenReturn(testMachine);

        ResponseEntity<Machine> response = catalogController.updateAvailability(1L, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(catalogService, times(1)).updateMachineAvailability(1L, false);
    }
    
    @Test
    void updateAvailability_ShouldReturnNotFoundWhenExceptionThrown() {
        when(catalogService.updateMachineAvailability(99L, false)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<Machine> response = catalogController.updateAvailability(99L, false);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
