package com.smartrent.report.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.stream.Collectors;

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
        if (message != null && message.contains("excavadora")) {
            return "Excavadora";
        } else if (message != null && message.contains("grua")) {
            return "Grua";
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
                .collect(Collectors.toList());
    }
}
