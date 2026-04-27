package com.aerospace.aitt.flashing.data;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.flashing.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Persists flashing sessions to JSON file.
 * Provides session history retrieval and search.
 */
public class SessionStore {
    
    private final AppLogger log = new AppLogger(SessionStore.class);
    private final Path sessionsFile;
    private final ObjectMapper objectMapper;
    private final Map<UUID, SessionData> sessions = new LinkedHashMap<>();
    
    public SessionStore() {
        Path dataDir = Path.of(System.getProperty("user.home"), ".aitt");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            log.error("Failed to create data directory", e);
        }
        this.sessionsFile = dataDir.resolve("sessions.json");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadSessions();
    }
    
    public SessionStore(Path sessionsFile) {
        this.sessionsFile = sessionsFile;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadSessions();
    }
    
    private void loadSessions() {
        if (!Files.exists(sessionsFile)) {
            log.info("No sessions file found, starting fresh");
            return;
        }
        try {
            String json = Files.readString(sessionsFile);
            List<SessionData> list = objectMapper.readValue(json, new TypeReference<List<SessionData>>() {});
            for (SessionData data : list) {
                sessions.put(data.id, data);
            }
            log.info("Loaded %d sessions from %s", sessions.size(), sessionsFile);
        } catch (IOException e) {
            log.error("Failed to load sessions", e);
        }
    }
    
    private void saveSessions() {
        try {
            String json = objectMapper.writeValueAsString(new ArrayList<>(sessions.values()));
            Files.writeString(sessionsFile, json);
        } catch (IOException e) {
            log.error("Failed to save sessions", e);
        }
    }
    
    /**
     * Saves a flashing session.
     */
    public void save(FlashingSession session) {
        Objects.requireNonNull(session, "session cannot be null");
        SessionData data = SessionData.fromSession(session);
        sessions.put(session.id(), data);
        saveSessions();
        log.info("Saved session: %s", session.id());
    }
    
    /**
     * Finds a session by ID.
     */
    public Optional<FlashingSession> findById(UUID id) {
        SessionData data = sessions.get(id);
        return data != null ? Optional.of(data.toSession()) : Optional.empty();
    }
    
    /**
     * Returns all sessions ordered by timestamp descending.
     */
    public List<FlashingSession> findAll() {
        return sessions.values().stream()
            .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
            .map(SessionData::toSession)
            .collect(Collectors.toList());
    }
    
    /**
     * Returns recent sessions (last N).
     */
    public List<FlashingSession> findRecent(int limit) {
        return sessions.values().stream()
            .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
            .limit(limit)
            .map(SessionData::toSession)
            .collect(Collectors.toList());
    }
    
    /**
     * Deletes a session by ID.
     */
    public void delete(UUID id) {
        sessions.remove(id);
        saveSessions();
        log.info("Deleted session: %s", id);
    }
    
    /**
     * Deletes all sessions.
     */
    public void deleteAll() {
        sessions.clear();
        saveSessions();
        log.info("Deleted all sessions");
    }
    
    // ========== Internal Data Classes ==========
    
    /**
     * JSON-serializable session data.
     */
    public static class SessionData {
        public UUID id;
        public List<ModuleData> modules;
        public String targetProfileId;
        public String targetProfileName;
        public String outputScriptPath;
        public Instant timestamp;
        public String status;
        public String errorMessage;
        public List<String> logs;
        
        public SessionData() {} // for Jackson
        
        public static SessionData fromSession(FlashingSession session) {
            SessionData data = new SessionData();
            data.id = session.id();
            data.modules = session.modules().stream()
                .map(ModuleData::fromModule)
                .collect(Collectors.toList());
            // Null-safe access to targetProfile
            if (session.targetProfile() != null) {
                data.targetProfileId = session.targetProfile().id();
                data.targetProfileName = session.targetProfile().name();
            } else {
                data.targetProfileId = null;
                data.targetProfileName = "Unknown";
            }
            data.outputScriptPath = session.outputScriptPath() != null 
                ? session.outputScriptPath().toString() : null;
            data.timestamp = session.timestamp();
            data.status = session.status().name();
            data.errorMessage = session.errorMessage();
            data.logs = new ArrayList<>(session.logs());
            return data;
        }
        
        public FlashingSession toSession() {
            List<SoftwareModule> moduleList = modules.stream()
                .map(ModuleData::toModule)
                .collect(Collectors.toList());
            
            TargetProfile profile = new TargetProfile(
                targetProfileId,
                targetProfileName,
                "Unknown",
                0x00000000L,
                0xFFFFFFFFL,
                0x20000000L,
                null,
                ""
            );
            
            // Safe status parsing with fallback
            FlashStatus flashStatus;
            try {
                flashStatus = FlashStatus.valueOf(status);
            } catch (IllegalArgumentException | NullPointerException e) {
                flashStatus = FlashStatus.FAILED;
            }
            
            return new FlashingSession(
                id,
                moduleList,
                profile,
                outputScriptPath != null ? Path.of(outputScriptPath) : null,
                timestamp,
                flashStatus,
                errorMessage,
                logs,
                null // remoteProbeId - not stored in legacy sessions
            );
        }
    }
    
    /**
     * JSON-serializable module data.
     */
    public static class ModuleData {
        public UUID id;
        public String name;
        public String filePath;
        public long flashAddress;
        public long size;
        public String checksum;
        
        public ModuleData() {} // for Jackson
        
        public static ModuleData fromModule(SoftwareModule module) {
            ModuleData data = new ModuleData();
            data.id = module.id();
            data.name = module.name();
            data.filePath = module.filePath().toString();
            data.flashAddress = module.flashAddress();
            data.size = module.size();
            data.checksum = module.checksum();
            return data;
        }
        
        public SoftwareModule toModule() {
            return new SoftwareModule(
                id,
                name,
                Path.of(filePath),
                flashAddress,
                size,
                checksum
            );
        }
    }
}
