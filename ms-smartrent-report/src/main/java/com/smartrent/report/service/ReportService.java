package com.smartrent.report.service;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.dto.RentalEventDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportService {

    private static final int MAX_RECENT_CATALOG_EVENTS = 20;
    private static final String GENERIC_MACHINE = "Maquina_Generica";
    private static final Pattern MACHINE_NAME_PATTERN =
            Pattern.compile("\"machineName\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final List<Map.Entry<String, List<String>>> MACHINE_KEYWORDS = List.of(
            Map.entry("Retroexcavadora", List.of("retroexcavadora", "\"machineid\":2")),
            Map.entry("Excavadora", List.of("excavadora", "\"machineid\":1", "\"rentalid\":1")),
            Map.entry("Grua", List.of("grua", "grúa", "\"machineid\":7", "\"machineid\":8")),
            Map.entry("Rodillo Compactador", List.of("rodillo", "\"machineid\":3")),
            Map.entry("Generador Industrial", List.of("generador", "\"machineid\":9", "\"machineid\":10")),
            Map.entry("Martillo Demoledor", List.of("demoledor", "\"machineid\":5"))
    );

    private final AtomicInteger dailyRentals = new AtomicInteger(0);
    private final Map<String, Integer> machineRequests = new ConcurrentHashMap<>();
    private final Map<String, Integer> rentalsByStatus = new ConcurrentHashMap<>();

    private final AtomicInteger catalogEventsTotal = new AtomicInteger(0);
    private final Map<String, Integer> catalogEventsByType = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> machineAvailability = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<CatalogEventDto> recentCatalogEvents = new ConcurrentLinkedDeque<>();

    private static Integer sumCounts(Integer a, Integer b) {
        return Objects.requireNonNull(a) + Objects.requireNonNull(b);
    }

    public void recordRentalEvent(String message) {
        dailyRentals.incrementAndGet();

        String machineName = extractMachineFromMessage(message);
        machineRequests.merge(machineName, 1, ReportService::sumCounts);
    }

    public void recordRentalEvent(RentalEventDto event) {
        if (event == null || event.getRentalId() == null
                || event.getStatus() == null || event.getStatus().isBlank()) {
            throw new IllegalArgumentException("Evento de arriendo inválido para reportería: rentalId y status requeridos");
        }
        dailyRentals.incrementAndGet();
        String machineName = resolveMachineName(event);
        machineRequests.merge(machineName, 1, ReportService::sumCounts);
        rentalsByStatus.merge(event.getStatus().trim().toUpperCase(Locale.ROOT), 1, ReportService::sumCounts);
    }

    public void recordCatalogEvent(CatalogEventDto event) {
        if (event == null || event.getMachineId() == null
                || event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Evento de catálogo inválido para reportería: machineId y eventType requeridos");
        }
        catalogEventsTotal.incrementAndGet();
        String normalizedType = event.getEventType().trim().toUpperCase(Locale.ROOT);
        catalogEventsByType.merge(normalizedType, 1, ReportService::sumCounts);

        boolean available = !"MACHINE_DELETED".equals(normalizedType) && Boolean.TRUE.equals(event.getAvailable());
        machineAvailability.put(event.getMachineId(), available);

        recentCatalogEvents.addFirst(event);
        while (recentCatalogEvents.size() > MAX_RECENT_CATALOG_EVENTS) {
            recentCatalogEvents.pollLast();
        }
    }

    private String resolveMachineName(RentalEventDto event) {
        String explicit = event.getMachineName();
        if (explicit != null && !explicit.isBlank() && !GENERIC_MACHINE.equalsIgnoreCase(explicit.trim())) {
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
                default -> GENERIC_MACHINE;
            };
        }
        return GENERIC_MACHINE;
    }

    private String extractMachineFromMessage(String message) {
        if (message == null) {
            return GENERIC_MACHINE;
        }
        Matcher matcher = MACHINE_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            String explicitName = matcher.group(1).trim();
            if (!explicitName.isEmpty() && !GENERIC_MACHINE.equalsIgnoreCase(explicitName)) {
                return explicitName;
            }
        }

        String lower = message.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : MACHINE_KEYWORDS) {
            if (entry.getValue().stream().anyMatch(lower::contains)) {
                return entry.getKey();
            }
        }
        return GENERIC_MACHINE;
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
