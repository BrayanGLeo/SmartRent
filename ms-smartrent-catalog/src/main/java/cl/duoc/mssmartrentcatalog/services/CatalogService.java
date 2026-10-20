package cl.duoc.mssmartrentcatalog.services;

import cl.duoc.mssmartrentcatalog.entities.Category;
import cl.duoc.mssmartrentcatalog.entities.Machine;
import cl.duoc.mssmartrentcatalog.repositories.CategoryRepository;
import cl.duoc.mssmartrentcatalog.repositories.MachineRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class CatalogService {

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_CATEGORY_ID = "categoryId";

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
        if (categoryRepository == null) {
            return machineRepository.save(Objects.requireNonNull(machine));
        }

        machine.setCategory(resolveCategory(machine.getCategory(), null));
        Optional<Machine> existing = findBySerialIfPresent(machine.getSerialNumber());
        if (existing.isPresent()) {
            return updateExistingFromMachine(Objects.requireNonNull(existing.get()), machine);
        }
        return machineRepository.save(Objects.requireNonNull(machine));
    }

    public Optional<Machine> getMachineById(@org.springframework.lang.NonNull Long id) {
        return machineRepository.findById(id);
    }

    public Machine updateMachineAvailability(@org.springframework.lang.NonNull Long id, Boolean isAvailable) {
        Optional<Machine> optionalMachine = machineRepository.findById(id);
        if (optionalMachine.isPresent()) {
            Machine machine = optionalMachine.get();
            machine.setIsAvailable(isAvailable);
            return machineRepository.save(Objects.requireNonNull(machine));
        }
        throw new jakarta.persistence.EntityNotFoundException("Machine not found with id " + id);
    }

    public Machine updateMachine(@org.springframework.lang.NonNull Long id, Map<String, Object> payload) {
        String skuOrSerial = extractString(payload, "serialNumber", "sku");
        Machine machine = findOrInitMachine(id, skuOrSerial);

        applyBasicFields(machine, payload, skuOrSerial);
        applyCategoryField(machine, payload);

        return machineRepository.save(Objects.requireNonNull(machine));
    }

    public void deleteMachine(@org.springframework.lang.NonNull Long id) {
        if (machineRepository.existsById(id)) {
            machineRepository.deleteById(id);
        }
    }

    private Optional<Machine> findBySerialIfPresent(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            return Optional.empty();
        }
        return machineRepository.findBySerialNumberIgnoreCase(serialNumber.trim());
    }

    private Machine updateExistingFromMachine(@org.springframework.lang.NonNull Machine target, Machine source) {
        if (source.getName() != null && !source.getName().isBlank()) {
            target.setName(source.getName());
        }
        if (source.getDailyPrice() != null) {
            target.setDailyPrice(source.getDailyPrice());
        }
        if (source.getIsAvailable() != null) {
            target.setIsAvailable(source.getIsAvailable());
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
        return machineRepository.save(target);
    }

    private Machine findOrInitMachine(@org.springframework.lang.NonNull Long id, String skuOrSerial) {
        Optional<Machine> optionalMachine = machineRepository.findById(id);
        if (optionalMachine.isEmpty()) {
            optionalMachine = findBySerialIfPresent(skuOrSerial);
        }
        return optionalMachine.orElseGet(() -> buildFallbackMachine(id, skuOrSerial));
    }

    private Machine buildFallbackMachine(Long id, String skuOrSerial) {
        Machine m = new Machine();
        m.setName("Equipo Industrial #" + id);
        m.setSerialNumber(skuOrSerial != null && !skuOrSerial.isBlank() ? skuOrSerial.trim() : "SKU-" + id);
        m.setDailyPrice(0.0);
        m.setIsAvailable(true);
        if (categoryRepository != null) {
            m.setCategory(resolveCategory(null, null));
        }
        return m;
    }

    private static void applyBasicFields(Machine machine, Map<String, Object> payload, String skuOrSerial) {
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
        extractAvailability(payload).ifPresent(machine::setIsAvailable);
    }

    private void applyCategoryField(Machine machine, Map<String, Object> payload) {
        if (categoryRepository == null) {
            return;
        }
        if (payload != null && (payload.containsKey(KEY_CATEGORY) || payload.containsKey(KEY_CATEGORY_ID))) {
            Long catId = extractLong(payload, KEY_CATEGORY_ID);
            Category ref = null;
            if (catId != null) {
                ref = new Category();
                ref.setId(catId);
            }
            String catName = extractCategoryName(payload.get(KEY_CATEGORY));
            machine.setCategory(resolveCategory(ref, catName));
        } else if (machine.getCategory() == null) {
            machine.setCategory(resolveCategory(null, null));
        }
    }

    private Category resolveCategory(Category requested, String categoryNameOverride) {
        if (categoryRepository == null) {
            return requested;
        }

        Long requestedId = requested != null ? requested.getId() : null;
        if (requestedId != null) {
            Optional<Category> byId = categoryRepository.findById(requestedId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String nameToSearch = resolveCategorySearchName(requested, categoryNameOverride);
        if (nameToSearch != null && !nameToSearch.isBlank()) {
            return findOrCreateCategoryByName(nameToSearch.trim());
        }

        return getDefaultOrFirstCategory();
    }

    private static String resolveCategorySearchName(Category requested, String categoryNameOverride) {
        if (categoryNameOverride != null && !categoryNameOverride.isBlank()) {
            return categoryNameOverride;
        }
        return requested != null ? requested.getName() : null;
    }

    private Category findOrCreateCategoryByName(String trimmedName) {
        return categoryRepository.findByNameIgnoreCase(trimmedName).orElseGet(() -> {
            Category created = new Category();
            created.setName(trimmedName);
            created.setDescription("Categoría " + trimmedName);
            return categoryRepository.save(created);
        });
    }

    private Category getDefaultOrFirstCategory() {
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
            Double parsed = parseDoubleObject(payload.get(key));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Double parseDoubleObject(Object val) {
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long extractLong(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Long parsed = parseLongObject(payload.get(key));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Long parseLongObject(Object val) {
        if (val instanceof Number num) {
            return num.longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Optional<Boolean> extractAvailability(Map<String, Object> payload) {
        if (payload == null) {
            return Optional.empty();
        }
        Object isAvailObj = payload.get("isAvailable");
        if (isAvailObj instanceof Boolean b) {
            return Optional.of(b);
        }
        if (isAvailObj != null) {
            return Optional.of(Boolean.parseBoolean(isAvailObj.toString()));
        }

        String status = extractString(payload, "status");
        Double availStock = extractDouble(payload, "availableStock");
        if (status != null) {
            return evaluateStatusAvailability(status, availStock);
        }
        if (availStock != null) {
            return Optional.of(availStock > 0);
        }
        return Optional.empty();
    }

    private static Optional<Boolean> evaluateStatusAvailability(String status, Double availStock) {
        if ("SIN_STOCK".equalsIgnoreCase(status)
                || "EN_MANTENCION".equalsIgnoreCase(status)
                || "ARRENDADO".equalsIgnoreCase(status)) {
            return Optional.of(false);
        }
        if ("DISPONIBLE".equalsIgnoreCase(status)) {
            return Optional.of(availStock == null || availStock > 0);
        }
        if (availStock != null) {
            return Optional.of(availStock > 0);
        }
        return Optional.empty();
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
