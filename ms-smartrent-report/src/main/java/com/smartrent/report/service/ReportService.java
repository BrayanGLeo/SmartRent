package com.smartrent.report.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

@Service
public class ReportService {

    private final AtomicInteger dailyRentals = new AtomicInteger(0);
    private final Map<String, Integer> machineRequests = new ConcurrentHashMap<>();

    public void recordRentalEvent(String message) {
        dailyRentals.incrementAndGet();
        
        // Simulación: Extraer nombre de máquina del mensaje (ej. si el mensaje incluye "maquinaId")
        // Como es una prueba, vamos a registrar una máquina de ejemplo u obtener algo simple.
        String machineName = extractMachineFromMessage(message);
        machineRequests.put(machineName, machineRequests.getOrDefault(machineName, 0) + 1);
    }

    private String extractMachineFromMessage(String message) {
        if (message == null) {
            return "Maquina_Generica";
        }
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("excavadora") || lower.contains("\"machineid\":1") || lower.contains("\"rentalid\":1")) {
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
