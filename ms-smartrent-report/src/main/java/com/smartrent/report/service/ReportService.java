package com.smartrent.report.service;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.dto.RentalEventDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportService {

    private static final int MAX_RECENT_CATALOG_EVENTS = 20;
    private static final Pattern MACHINE_NAME_PATTERN =
            Pattern.compile("\"machineName\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final AtomicInteger dailyRentals = new AtomicInteger(0);
    private final Map<String, Integer> machineRequests = new ConcurrentHashMap<>();
    private final Map<String, Integer> rentalsByStatus = new ConcurrentHashMap<>();

    private final AtomicInteger catalogEventsTotal = new AtomicInteger(0);
    private final Map<String, Integer> catalogEventsByType = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> machineAvailability = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<CatalogEventDto> recentCatalogEvents = new ConcurrentLinkedDeque<>();

    public void recordRentalEvent(String message) {
        dailyRentals.incrementAndGet();

        String machineName = extractMachineFromMessage(message);
        machineRequests.merge(machineName, 1, Integer::sum);
    }

    public void recordRentalEvent(RentalEventDto event) {
        if (event == null || event.getRentalId() == null
                || event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("Evento de arriendo inválido para reportería: rentalId y status requeridos");
        }
        dailyRentals.incrementAndGet();
        String machineName = resolveMachineName(event);
        machineRequests.merge(machineName, 1, Integer::sum);
        rentalsByStatus.merge(event.getStatus().trim().toUpperCase(Locale.ROOT), 1, Integer::sum);
    }

    public void recordCatalogEvent(CatalogEventDto event) {
        if (event == null || event.getMachineId() == null
                || event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Evento de catálogo inválido para reportería: machineId y eventType requeridos");
        }
        catalogEventsTotal.incrementAndGet();
        String normalizedType = event.getEventType().trim().toUpperCase(Locale.ROOT);
        catalogEventsByType.merge(normalizedType, 1, Integer::sum);

        boolean available = !"MACHINE_DELETED".equals(normalizedType) && Boolean.TRUE.equals(event.getAvailable());
        machineAvailability.put(event.getMachineId(), available);

        recentCatalogEvents.addFirst(event);
        while (recentCatalogEvents.size() > MAX_RECENT_CATALOG_EVENTS) {
            recentCatalogEvents.pollLast();
        }
    }

    private String resolveMachineName(RentalEventDto event) {
        String explicit = event.getMachineName();
        if (explicit != null && !explicit.isBlank() && !"Maquina_Generica".equalsIgnoreCase(explicit.trim())) {
            return explicit.trim();
        }
        if (event.getMachineId() != null) {
            return switch (event.getMachineId().intValue()) {
                case 1 -> "Excavadora";
                case 2 -> "Retroexcavadora";
                case 3 -> "Rodillo Compactador";
                case 5 -> "Martillo Demoledor";
                case 7, 8 -> "Grua";
                case 9, 10 -> "Generador Industrial";
                default -> "Maquina_Generica";
            };
        }
        return "Maquina_Generica";
    }

    private String extractMachineFromMessage(String message) {
        if (message == null) {
            return "Maquina_Generica";
        }
        Matcher matcher = MACHINE_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            String explicitName = matcher.group(1).trim();
            if (!explicitName.isEmpty() && !"Maquina_Generica".equalsIgnoreCase(explicitName)) {
                return explicitName;
            }
        }

        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("retroexcavadora") || lower.contains("\"machineid\":2")) {
            return "Retroexcavadora";
        } else if (lower.contains("excavadora") || lower.contains("\"machineid\":1") || lower.contains("\"rentalid\":1")) {
            return "Excavadora";
        } else if (lower.contains("grua") || lower.contains("grúa") || lower.contains("\"machineid\":7") || lower.contains("\"machineid\":8")) {
            return "Grua";
        } else if (lower.contains("rodillo") || lower.contains("\"machineid\":3")) {
            return "Rodillo Compactador";
        } else if (lower.contains("generador") || lower.contains("\"machineid\":9") || lower.contains("\"machineid\":10")) {
            return "Generador Industrial";
        } else if (lower.contains("demoledor") || lower.contains("\"machineid\":5")) {
            return "Martillo Demoledor";
        }
        return "Maquina_Generica";
    }

    public int getDailyRentals() {
        return dailyRentals.get();
    }

    public List<Map.Entry<String, Integer>> getTopServices() {
        return machineRequests.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .toList();
    }

    public Map<String, Integer> getRentalsByStatus() {
        return Map.copyOf(rentalsByStatus);
    }

    public Map<String, Object> getCatalogActivitySummary() {
        long availableCount = machineAvailability.values().stream()
                .filter(Boolean.TRUE::equals)
                .count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEvents", catalogEventsTotal.get());
        summary.put("eventsByType", Map.copyOf(catalogEventsByType));
        summary.put("availableMachinesCount", availableCount);
        summary.put("trackedMachines", machineAvailability.size());
        summary.put("recentEvents", List.copyOf(recentCatalogEvents));
        return summary;
    }
}
