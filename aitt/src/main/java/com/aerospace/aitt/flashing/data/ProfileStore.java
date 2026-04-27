package com.aerospace.aitt.flashing.data;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.flashing.model.TargetProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and manages target profiles from JSON configuration.
 * Profiles are read-only and loaded from resources or external file.
 */
public class ProfileStore {
    
    private final AppLogger log = new AppLogger(ProfileStore.class);
    
    private static final String DEFAULT_PROFILES_RESOURCE = "/profiles/targets.json";
    
    private final ObjectMapper objectMapper;
    private final Map<String, TargetProfile> profiles = new LinkedHashMap<>();
    
    public ProfileStore() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        loadProfiles();
    }
    
    /**
     * Creates a ProfileStore with an external profiles file.
     */
    public ProfileStore(Path profilesPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        loadProfiles(profilesPath);
    }
    
    /**
     * Returns all available profiles.
     */
    public List<TargetProfile> findAll() {
        return new ArrayList<>(profiles.values());
    }
    
    /**
     * Finds a profile by ID.
     */
    public Optional<TargetProfile> findById(String id) {
        return Optional.ofNullable(profiles.get(id));
    }
    
    /**
     * Finds profiles by CPU type.
     */
    public List<TargetProfile> findByCpuType(String cpuType) {
        return profiles.values().stream()
            .filter(p -> p.cpuType().equalsIgnoreCase(cpuType))
            .toList();
    }
    
    /**
     * Returns the number of profiles.
     */
    public int count() {
        return profiles.size();
    }
    
    /**
     * Reloads profiles from the default source.
     */
    public void reload() {
        profiles.clear();
        loadProfiles();
    }
    
    /**
     * Loads profiles from classpath resource.
     */
    private void loadProfiles() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_PROFILES_RESOURCE)) {
            if (is == null) {
                log.warn("Profiles resource not found: %s", DEFAULT_PROFILES_RESOURCE);
                loadDefaultProfiles();
                return;
            }
            
            List<ProfileJson> profileList = objectMapper.readValue(
                is, 
                new TypeReference<List<ProfileJson>>() {}
            );
            
            for (ProfileJson json : profileList) {
                TargetProfile profile = json.toTargetProfile();
                profiles.put(profile.id(), profile);
            }
            
            log.info("Loaded %d target profiles", profiles.size());
            
        } catch (IOException e) {
            log.error("Failed to load profiles", e);
            loadDefaultProfiles();
        }
    }
    
    /**
     * Loads profiles from external file.
     */
    private void loadProfiles(Path path) {
        try {
            if (!Files.exists(path)) {
                log.warn("Profiles file not found: %s", path);
                loadDefaultProfiles();
                return;
            }
            
            String json = Files.readString(path);
            List<ProfileJson> profileList = objectMapper.readValue(
                json, 
                new TypeReference<List<ProfileJson>>() {}
            );
            
            for (ProfileJson pj : profileList) {
                TargetProfile profile = pj.toTargetProfile();
                profiles.put(profile.id(), profile);
            }
            
            log.info("Loaded %d target profiles from %s", profiles.size(), path);
            
        } catch (IOException e) {
            log.error("Failed to load profiles from file", e);
            loadDefaultProfiles();
        }
    }
    
    /**
     * Loads hardcoded default profiles as fallback.
     */
    private void loadDefaultProfiles() {
        log.info("Loading default profiles");
        
        profiles.put("cortex-r5-mcu", new TargetProfile(
            "cortex-r5-mcu",
            "Cortex-R5 MCU",
            "CortexR5",
            0x08000000L,
            0x08200000L,
            0x20000000L,
            null,
            "ARM Cortex-R5 Safety MCU with 2MB Flash"
        ));
        
        profiles.put("cortex-m4-safety", new TargetProfile(
            "cortex-m4-safety",
            "Cortex-M4 Safety",
            "CortexM4",
            0x08000000L,
            0x08100000L,
            0x20000000L,
            null,
            "ARM Cortex-M4 Safety Controller with 1MB Flash"
        ));
        
        profiles.put("cortex-m7-perf", new TargetProfile(
            "cortex-m7-perf",
            "Cortex-M7 Performance",
            "CortexM7",
            0x08000000L,
            0x08200000L,
            0x20000000L,
            null,
            "ARM Cortex-M7 High Performance MCU with 2MB Flash"
        ));
        
        profiles.put("cortex-a53-app", new TargetProfile(
            "cortex-a53-app",
            "Cortex-A53 Application",
            "CortexA53",
            0x00000000L,
            0x10000000L,
            0x80000000L,
            null,
            "ARM Cortex-A53 Application Processor"
        ));
    }
    
    /**
     * JSON mapping class for profile deserialization.
     */
    private static class ProfileJson {
        public String id;
        public String name;
        public String cpuType;
        public String flashStart;
        public String flashEnd;
        public String ramAddress;
        public String t32ConfigPath;
        public String description;
        
        public TargetProfile toTargetProfile() {
            return new TargetProfile(
                id,
                name,
                cpuType,
                parseHex(flashStart),
                parseHex(flashEnd),
                parseHex(ramAddress),
                t32ConfigPath != null ? Path.of(t32ConfigPath) : null,
                description
            );
        }
        
        private long parseHex(String hex) {
            if (hex == null || hex.isBlank()) return 0;
            String cleaned = hex.trim();
            if (cleaned.toLowerCase().startsWith("0x")) {
                cleaned = cleaned.substring(2);
            }
            return Long.parseUnsignedLong(cleaned, 16);
        }
    }
}
