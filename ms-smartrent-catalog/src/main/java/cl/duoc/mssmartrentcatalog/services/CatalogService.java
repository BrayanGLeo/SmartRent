package cl.duoc.mssmartrentcatalog.services;

import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.repositories.CategoryRepository;
import cl.duoc.mssmartrentcatalog.repositories.MachineRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CatalogService {

    private final MachineRepository machineRepository;
    private final CategoryRepository categoryRepository;

    public CatalogService(MachineRepository machineRepository) {
        this(machineRepository, null);
    }

    @Autowired
    public CatalogService(MachineRepository machineRepository, @Autowired(required = false) CategoryRepository categoryRepository) {
        this.machineRepository = machineRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Machine> getAllAvailableMachines() {
        return machineRepository.findByIsAvailableTrue();
    }

    public List<Machine> getAllMachines() {
        return machineRepository.findAll();
    }

    public Machine createMachine(Machine machine) {
        if (machine.getIsAvailable() == null) {
            machine.setIsAvailable(true);
        }
        if (categoryRepository != null) {
            machine.setCategory(resolveCategory(machine.getCategory(), null));
            if (machine.getSerialNumber() != null && !machine.getSerialNumber().isBlank()) {
                Optional<Machine> existing = machineRepository.findBySerialNumberIgnoreCase(machine.getSerialNumber().trim());
                if (existing.isPresent()) {
                    Machine found = existing.get();
                    if (machine.getName() != null && !machine.getName().isBlank()) {
                        found.setName(machine.getName());
                    }
                    if (machine.getDailyPrice() != null) {
                        found.setDailyPrice(machine.getDailyPrice());
                    }
                    if (machine.getIsAvailable() != null) {
                        found.setIsAvailable(machine.getIsAvailable());
                    }
                    if (machine.getCategory() != null) {
                        found.setCategory(machine.getCategory());
                    }
                    return machineRepository.save(found);
                }
            }
        }
        return machineRepository.save(machine);
    }

    public Optional<Machine> getMachineById(@org.springframework.lang.NonNull Long id) {
        return machineRepository.findById(id);
    }

    public Machine updateMachineAvailability(@org.springframework.lang.NonNull Long id, Boolean isAvailable) {
        Optional<Machine> optionalMachine = machineRepository.findById(id);
        if (optionalMachine.isPresent()) {
            Machine machine = optionalMachine.get();
            machine.setIsAvailable(isAvailable);
            return machineRepository.save(machine);
        }
        throw new jakarta.persistence.EntityNotFoundException("Machine not found with id " + id);
    }

    public Machine updateMachine(@org.springframework.lang.NonNull Long id, Map<String, Object> payload) {
        Optional<Machine> optionalMachine = machineRepository.findById(id);
        String skuOrSerial = extractString(payload, "serialNumber", "sku");

        if (optionalMachine.isEmpty() && skuOrSerial != null && !skuOrSerial.isBlank()) {
            optionalMachine = machineRepository.findBySerialNumberIgnoreCase(skuOrSerial.trim());
        }

        Machine machine = optionalMachine.orElseGet(() -> {
            Machine m = new Machine();
            m.setName("Equipo Industrial #" + id);
            m.setSerialNumber(skuOrSerial != null && !skuOrSerial.isBlank() ? skuOrSerial.trim() : "SKU-" + id);
            m.setDailyPrice(0.0);
            m.setIsAvailable(true);
            if (categoryRepository != null) {
                m.setCategory(resolveCategory(null, null));
            }
            return m;
        });

        String name = extractString(payload, "name");
        if (name != null && !name.isBlank()) {
            machine.setName(name.trim());
        }

        if (skuOrSerial != null && !skuOrSerial.isBlank()) {
            machine.setSerialNumber(skuOrSerial.trim());
        }

        Double price = extractDouble(payload, "dailyPrice", "dailyRate", "pricePerDay");
        if (price != null) {
            machine.setDailyPrice(price);
        }

        Boolean resolvedAvailable = extractAvailability(payload);
        if (resolvedAvailable != null) {
            machine.setIsAvailable(resolvedAvailable);
        }

        if (categoryRepository != null && (payload != null && (payload.containsKey("category") || payload.containsKey("categoryId")))) {
            Long catId = extractLong(payload, "categoryId");
            Category ref = null;
            if (catId != null) {
                ref = new Category();
                ref.setId(catId);
            }
            String catName = extractCategoryName(payload.get("category"));
            machine.setCategory(resolveCategory(ref, catName));
        } else if (categoryRepository != null && machine.getCategory() == null) {
            machine.setCategory(resolveCategory(null, null));
        }

        return machineRepository.save(machine);
    }

    public void deleteMachine(@org.springframework.lang.NonNull Long id) {
        if (machineRepository.existsById(id)) {
            machineRepository.deleteById(id);
        }
    }

    private Category resolveCategory(Category requested, String categoryNameOverride) {
        if (categoryRepository == null) {
            return requested;
        }

        if (requested != null && requested.getId() != null) {
            Optional<Category> byId = categoryRepository.findById(requested.getId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String nameToSearch = categoryNameOverride;
        if ((nameToSearch == null || nameToSearch.isBlank()) && requested != null) {
            nameToSearch = requested.getName();
        }

        if (nameToSearch != null && !nameToSearch.isBlank()) {
            String trimmed = nameToSearch.trim();
            Optional<Category> byName = categoryRepository.findByNameIgnoreCase(trimmed);
            if (byName.isPresent()) {
                return byName.get();
            }
            Category created = new Category();
            created.setName(trimmed);
            created.setDescription("Categoría " + trimmed);
            return categoryRepository.save(created);
        }

        List<Category> all = categoryRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }

        Category fallback = new Category();
        fallback.setName("Maquinaria Pesada");
        fallback.setDescription("Equipos industriales de arriendo");
        return categoryRepository.save(fallback);
    }

    private static String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return null;
    }

    private static Double extractDouble(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val instanceof Number num) {
                return num.doubleValue();
            }
            if (val != null) {
                try {
                    return Double.parseDouble(val.toString());
                } catch (NumberFormatException ignored) {
                    // continue
                }
            }
        }
        return null;
    }

    private static Long extractLong(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val instanceof Number num) {
                return num.longValue();
            }
            if (val != null) {
                try {
                    return Long.parseLong(val.toString());
                } catch (NumberFormatException ignored) {
                    // continue
                }
            }
        }
        return null;
    }

    private static Boolean extractAvailability(Map<String, Object> payload) {
        if (payload == null) return null;
        Object isAvailObj = payload.get("isAvailable");
        if (isAvailObj instanceof Boolean b) {
            return b;
        }
        if (isAvailObj != null) {
            return Boolean.parseBoolean(isAvailObj.toString());
        }

        String status = extractString(payload, "status");
        Double availStock = extractDouble(payload, "availableStock");
        if (status != null) {
            if ("SIN_STOCK".equalsIgnoreCase(status) || "EN_MANTENCION".equalsIgnoreCase(status) || "ARRENDADO".equalsIgnoreCase(status)) {
                return false;
            }
            if ("DISPONIBLE".equalsIgnoreCase(status)) {
                return availStock == null || availStock > 0;
            }
        }
        if (availStock != null) {
            return availStock > 0;
        }
        return null;
    }

    private static String extractCategoryName(Object categoryObj) {
        if (categoryObj == null) return null;
        if (categoryObj instanceof Map<?, ?> map) {
            Object name = map.get("name");
            return name != null ? name.toString() : null;
        }
        return categoryObj.toString();
    }
}
