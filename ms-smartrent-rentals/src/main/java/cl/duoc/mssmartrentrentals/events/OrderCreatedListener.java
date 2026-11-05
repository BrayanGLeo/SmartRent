package cl.duoc.mssmartrentrentals.events;

import cl.duoc.mssmartrentrentals.config.RabbitMQConfig;
import cl.duoc.mssmartrentrentals.entities.Rental;
import cl.duoc.mssmartrentrentals.services.RentalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private final RentalService rentalService;

    public OrderCreatedListener(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void onOrderCreated(Map<String, Object> event) {
        if (event == null) {
            return;
        }
        String userId = event.get("userId") != null ? event.get("userId").toString() : "unknown@user.com";
        Object itemsObj = event.get("items");
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (itemsObj instanceof List<?> items && !items.isEmpty()) {
            for (Object itemObj : items) {
                Long machineId = extractMachineId(itemObj);
                createRentalForMachine(userId, machineId, today);
            }
        } else {
            createRentalForMachine(userId, 1L, today);
        }
    }

    private void createRentalForMachine(String userId, Long machineId, LocalDate startDate) {
        try {
            Rental rental = new Rental();
            rental.setUserId(userId);
            rental.setMachineId(machineId != null ? machineId : 1L);
            rental.setStartDate(startDate);
            rental.setEndDate(startDate.plusDays(7));
            Rental created = rentalService.createRental(rental);
            log.info("Arriendo creado desde Checkout: id={}, machineId={}, userId={}",
                    created != null ? created.getId() : null, machineId, userId);
        } catch (Exception ex) {
            log.error("Error creando arriendo desde evento de Checkout para machineId={}: {}", machineId, ex.getMessage());
        }
    }

    private static Long extractMachineId(Object itemObj) {
        if (itemObj instanceof Map<?, ?> itemMap) {
            Object rawId = itemMap.get("machineId");
            if (rawId instanceof Number num) {
                return num.longValue();
            }
            if (rawId != null) {
                try {
                    return Long.parseLong(rawId.toString());
                } catch (NumberFormatException ignored) {
                    return 1L;
                }
            }
        }
        return 1L;
    }
}
