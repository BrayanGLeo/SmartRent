package cl.duoc.mssmartrentrentals.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-client", url = "${CATALOG_URL:http://smartrent-catalog:8081}")
public interface CatalogClient {

    @PutMapping("/api/catalog/services/{id}/availability")
    void updateAvailability(@PathVariable("id") Long id, @RequestParam("isAvailable") Boolean isAvailable);
}
