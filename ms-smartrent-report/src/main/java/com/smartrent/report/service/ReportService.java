package com.smartrent.report.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportService {

    private static final Pattern MACHINE_NAME_PATTERN =
            Pattern.compile("\"machineName\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final AtomicInteger dailyRentals = new AtomicInteger(0);
    private final Map<String, Integer> machineRequests = new ConcurrentHashMap<>();

    public void recordRentalEvent(String message) {
        dailyRentals.incrementAndGet();

        String machineName = extractMachineFromMessage(message);
        machineRequests.merge(machineName, 1, Integer::sum);
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
}
