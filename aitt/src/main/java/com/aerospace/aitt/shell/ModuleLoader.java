package com.aerospace.aitt.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Module loader and registry for application modules.
 * Manages module lifecycle and provides module discovery.
 */
public class ModuleLoader {
    
    private static final Logger log = LoggerFactory.getLogger(ModuleLoader.class);
    private static ModuleLoader instance;
    
    private final Map<String, ModuleDescriptor> modules = new java.util.concurrent.ConcurrentHashMap<>();
    
    private ModuleLoader() {
        registerBuiltInModules();
    }
    
    public static synchronized ModuleLoader getInstance() {
        if (instance == null) {
            instance = new ModuleLoader();
        }
        return instance;
    }
    
    /**
     * Registers the built-in modules.
     */
    private void registerBuiltInModules() {
        register(new ModuleDescriptor(
            "flashing",
            "Flashing Module",
            "Flash firmware to embedded chipsets using TRACE32",
            "/fxml/flashing.fxml",
            "flash-icon",
            true
        ));
        
        register(new ModuleDescriptor(
            "config",
            "Configuration Module",
            "Configure system parameters and profiles",
            "/fxml/config.fxml",
            "config-icon",
            true
        ));
        
        register(new ModuleDescriptor(
            "testdev",
            "Test Development Module",
            "Develop and manage test cases",
            "/fxml/testdev.fxml",
            "test-icon",
            true
        ));
        
        log.info("Registered {} built-in modules", modules.size());
    }
    
    /**
     * Registers a module.
     */
    public void register(ModuleDescriptor module) {
        modules.put(module.id(), module);
        log.debug("Registered module: {}", module.id());
    }
    
    /**
     * Gets a module by ID.
     */
    public Optional<ModuleDescriptor> getModule(String id) {
        return Optional.ofNullable(modules.get(id));
    }
    
    /**
     * Gets all registered modules.
     */
    public Map<String, ModuleDescriptor> getAllModules() {
        return Map.copyOf(modules);
    }
    
    /**
     * Checks if a module is enabled.
     */
    public boolean isEnabled(String moduleId) {
        ModuleDescriptor module = modules.get(moduleId);
        return module != null && module.enabled();
    }
    
    /**
     * Module descriptor containing metadata and configuration.
     */
    public record ModuleDescriptor(
        String id,
        String name,
        String description,
        String fxmlPath,
        String iconClass,
        boolean enabled
    ) {}
}
