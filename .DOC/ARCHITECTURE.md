# AITT Architecture Overview

## Project Summary

**AITT (Automation Integration Test Tool)** is a JavaFX desktop application for integration testing of aeronautical embedded systems. It provides comprehensive firmware flashing, hardware debugging, and remote probe management capabilities.

---

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER (JavaFX)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │   Login      │  │  Dashboard   │  │   Flashing   │  [Hardware]   │
│  │ Controller   │  │  Controller  │  │  Controller  │  [Remote]     │
│  └──────────────┘  └──────────────┘  └──────────────┘  [Scripting]  │
│  [Settings]  [Config]  [TestDev]                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│               APPLICATION LOGIC & SERVICES LAYER                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────┐  ┌──────────────────┐                      │
│  │  FlashingService    │  │ HardwareService  │                      │
│  ├─────────────────────┤  ├──────────────────┤                      │
│  │ • Orchestration     │  │ • Async ops      │  NetworkProbeService │
│  │ • Session mgmt      │  │ • State mgmt     │  AuthService         │
│  │ • Validation        │  │ • Event listeners│                      │
│  │ • Script generation │  │ • Reconnection   │                      │
│  └─────────────────────┘  └──────────────────┘                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│            BUSINESS LOGIC & DOMAIN MODELS LAYER                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Flashing Domain:        Hardware Domain:      Network Domain:      │
│  • FlashingSession       • HardwareInfo        • NetworkProbe       │
│  • SoftwareModule        • CommandResult       • RemoteType         │
│  • TargetProfile         • ChipsetConfig       • ConnectionStatus   │
│  • FlashStatus           • NativeException                          │
│  • ValidationResult              └─ User, AuthResult                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│              SUPPORTING LAYERS                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Configuration:    Data Management:   UI Framework:   Utilities:    │
│  • AppSettings     • SessionStore     • StageManager  • HexUtil     │
│  • SettingsStore   • ProfileStore     • BaseController • FileUtil   │
│                                       • ThemeManager  • ChecksumUtil│
│  Logging:                             • Alerts                      │
│  • AppLogger                          • InfiniteCarousel            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ JNI
┌─────────────────────────────────────────────────────────────────────┐
│              NATIVE LAYER (C/C++ - aitt_native.dll)                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │ HardwareBridge (JNI Wrapper)                             │       │
│  │ • init()  • discoverProbes()  • connect()               │       │
│  │ • sendCommand()  • readMemory()  • writeMemory()        │       │
│  └──────────────────────────────────────────────────────────┘       │
│                              ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │ Native Core (aitt_native.c)                              │       │
│  │ • Probe enumeration  • Protocol handling                │       │
│  │ • Memory management  • Driver registration              │       │
│  └──────────────────────────────────────────────────────────┘       │
│                              ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │ Chipset Drivers                                          │       │
│  ├──────────────────────┬──────────────────────────────────┤       │
│  │ chipset_s32k148.c    │ chipset_mpc5777m.c              │       │
│  │ (ARM Cortex-M4F)     │ (Power ISA Dual-Core)           │       │
│  │ SWD Debug Interface  │ JTAG Debug Interface            │       │
│  └──────────────────────┴──────────────────────────────────┘       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│              EXTERNAL HARDWARE & SYSTEMS                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Local:                      Remote:           External Tools:      │
│  • USB Debug Probes          • Network Probes  • TRACE32            │
│  • Serial Ports              • TCP Sockets     • Chipset Firmware   │
│  • JTAG/SWD Adapters         • mDNS Services   • File System        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Feature Modules & Responsibilities

### 1. **Firmware Flashing Module**
```
com.aerospace.aitt.flashing/
├── service/
│   ├── FlashingService      (Orchestration)
│   ├── Validator            (Validation logic)
│   ├── CmmGenerator         (Script generation)
│   └── Trace32Runner        (Process execution)
├── model/
│   ├── FlashingSession      (State representation)
│   ├── SoftwareModule       (Firmware module)
│   ├── TargetProfile        (Memory map)
│   ├── FlashStatus          (Status enum)
│   └── ValidationResult     (Validation outcome)
├── data/
│   ├── SessionStore         (Session persistence)
│   └── ProfileStore         (Profile loading)
└── ui/
    └── FlashingController   (UI Logic)
```

**Workflow:**
```
User Input → Validation → CMM Generation → TRACE32 Execution → Archive
     ↓            ↓              ↓                ↓                ↓
   UI         Validator    CmmGenerator    Trace32Runner      SessionStore
```

### 2. **Hardware Management Module**
```
com.aerospace.aitt.hardware/
├── ui/
│   └── HardwareController   (UI Logic)
└── Dependencies:
    ├── HardwareService      (High-level wrapper)
    └── HardwareBridge       (JNI interface)
```

**Probe Discovery Flow:**
```
HardwareService → JNI Call → aitt_native.dll → USB/Serial Enum
                                    ↓
                            Chipset Drivers (S32K148, MPC5777M)
                                    ↓
                            ProbeInfo Objects → UI Display
```

### 3. **Remote Debugging Module**
```
com.aerospace.aitt.remote/
├── service/
│   ├── NetworkProbeService  (Discovery & Management)
│   └── RemoteFlashingService (Remote operations)
├── model/
│   └── NetworkProbe         (Network probe entity)
└── ui/
    └── RemoteProbeController (UI Logic)
```

**Discovery Methods:**
- UDP broadcast for AITT probe servers
- Direct TCP connection to known addresses
- Subnet scanning for TRACE32 units
- mDNS/DNS-SD discovery

### 4. **Core Infrastructure**
```
com.aerospace.aitt.core/
├── auth/
│   ├── AuthService          (Authentication)
│   └── User                 (User entity)
├── config/
│   ├── AppSettings          (Settings model)
│   ├── SettingsStore        (Persistence)
│   └── ui/SettingsController (Settings UI)
├── ui/
│   ├── StageManager         (Scene navigation - Singleton)
│   ├── BaseController       (Controller base class)
│   ├── ThemeManager         (Dark/Light themes)
│   ├── Alerts               (Dialog utilities)
│   └── InfiniteCarousel     (UI component)
├── log/
│   └── AppLogger            (Logging facade)
└── util/
    ├── HexUtil              (Hex formatting)
    ├── FileUtil             (File operations)
    └── ChecksumUtil         (Hash computation)
```

---

## Data Model Entity Relationship

```
┌────────────────────┐
│  FlashingSession   │
├────────────────────┤
│ - id               │
│ - modules[]        ├──────┐
│ - targetProfile    │      │
│ - status           │      │
│ - generatedScript  │      │
│ - startTime        │      │
│ - endTime          │      │
└────────────────────┘      │
                             │
                    ┌────────▼──────────┐
                    │ SoftwareModule    │
                    ├───────────────────┤
                    │ - name            │
                    │ - version         │
                    │ - binaryPath      │
                    │ - baseAddress     │
                    │ - size            │
                    │ - checksum        │
                    └───────────────────┘
                             │
                    ┌────────▼──────────┐
                    │ TargetProfile     │
                    ├───────────────────┤
                    │ - id              │
                    │ - cpuType         │
                    │ - flashStart      │
                    │ - flashEnd        │
                    │ - ramAddress      │
                    │ - t32ConfigPath   │
                    └───────────────────┘

┌────────────────────┐
│  User              │
├────────────────────┤
│ - username         │
│ - displayName      │
│ - role             │
│ - loginTime        │
└────────────────────┘

┌────────────────────┐
│  NetworkProbe      │
├────────────────────┤
│ - id               │
│ - hostname         │
│ - ipAddress        │
│ - port             │
│ - probeType        │
│ - connectionStatus │
│ - lastSeen         │
└────────────────────┘

┌────────────────────┐
│  HardwareInfo      │
├────────────────────┤
│ - probeId          │
│ - probeType        │
│ - serialNumber     │
│ - firmwareVersion  │
│ - supportedChipsets│
└────────────────────┘
```

---

## Service Layer Interaction Pattern

```
┌──────────────────────────────────────────────────────────┐
│ FlashingService (Orchestrator)                           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  execute(modules, target, ...) {                         │
│    1. ValidationResult = validator.validate(...)         │
│    2. CmmScript = generator.generate(...)                │
│    3. Session = session.create(...)                      │
│    4. Result = runner.execute(cmmScript)                 │
│    5. sessionStore.save(session)                         │
│    return session                                        │
│  }                                                       │
│                                                          │
│  Async Processing: CompletableFuture<FlashingSession>   │
│  Progress Callbacks: Consumer<Double>                    │
│  Log Callbacks: Consumer<String>                         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## Package Dependencies (Simplified Dependency Graph)

```
shell/ ──→ core/ui, core/auth, native_/
    ↓
core/ ──→ core/log, core/util
    ↓
flashing/ ──→ flashing/data, core/log, core/util, core/ui
    ↓
flashing/ui ──→ flashing/service, core/ui/Alerts
    ↓
hardware/ui ──→ native_/HardwareService, core/ui, core/log
    ↓
remote/ui ──→ remote/service, remote/model, core/ui, core/log
    ↓
native_/ ──→ native library (aitt_native.dll - JNI)
    ↓
└──→ All services depend on: core/log/AppLogger

No circular dependencies
Clear separation of concerns
```

---

## Native Library Architecture (C/C++)

```
┌────────────────────────────────────────────────────────┐
│ aitt_native.dll (x64 Windows)                          │
├────────────────────────────────────────────────────────┤
│                                                        │
│  JNI Interface Layer                                   │
│  ├─ HardwareBridge.java ←→ jni_bindings.c             │
│  │  * Methods converted to native calls                │
│  │  * Type marshalling (Java ↔ C)                      │
│  │  * Exception handling                               │
│  │                                                     │
│  Core Implementation Layer                             │
│  ├─ aitt_native.c                                      │
│  │  * Initialization & cleanup                         │
│  │  * Probe enumeration                                │
│  │  * Protocol management                              │
│  │  * Driver registration                              │
│  │                                                     │
│  Hardware Driver Layer                                 │
│  ├─ chipset_s32k148.c (ARM Cortex-M4F - SWD)          │
│  │  * SWD protocol implementation                      │
│  │  * Memory read/write                                │
│  │  * Register access                                  │
│  │                                                     │
│  ├─ chipset_mpc5777m.c (Power ISA - JTAG)             │
│  │  * JTAG protocol implementation                     │
│  │  * Dual-core synchronization                        │
│  │  * Memory operations                                │
│  │                                                     │
│  Platform Layer                                        │
│  └─ USB, Serial, JTAG, SWD interfaces                  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

## Build System & Dependencies

```
Maven Project Structure:
├── pom.xml
│   ├── JavaFX 20.0.2 (UI Framework)
│   ├── Jackson 2.16.1 (JSON serialization)
│   ├── Logback 1.4.14 (Logging)
│   └── JUnit 5.10.2 (Testing)
│
├── src/main/java/
│   └── com/aerospace/aitt/
│
├── src/main/resources/
│   ├── fxml/ (FXML layouts)
│   ├── css/ (Stylesheets)
│   ├── native/windows-x64/ (aitt_native.dll)
│   ├── profiles/ (Chipset profiles - targets.json)
│   └── sounds/ (Audio resources)
│
├── src/test/java/
│   └── Unit tests
│
└── natice/
    ├── CMakeLists.txt
    ├── src/
    │   ├── aitt_native.c (Core)
    │   ├── jni_bindings.c (JNI)
    │   ├── chipset_s32k148.c
    │   └── chipset_mpc5777m.c
    └── build/ (CMake output)
```

---

## Configuration & Settings Flow

```
AppSettings (POJO)
      ↓
Jackson Serialization
      ↓
settings.json (File System)
      ↓
SettingsStore (Persistence Layer)
      ↓
Consumed by:
├── ThemeManager (theme property)
├── FlashingService (defaultTargetProfile, etc.)
├── HardwareService (No direct dependency)
└── UI Controllers (SettingsController reads/writes)
```

---

## Extension Points

### 1. Adding New Chipsets
```
Step 1: Create native/src/chipset_<name>.c
Step 2: Implement ChipsetDriver interface
Step 3: Add to CMakeLists.txt & aitt_native.c
Step 4: Create TargetProfile JSON
Step 5: Add to profiles/targets.json
Step 6: Test via Hardware module
```

### 2. Adding New UI Modules
```
Step 1: Create package com.aerospace.aitt.<module>
Step 2: Create <Module>Controller (extends BaseController)
Step 3: Create <module>.fxml resource
Step 4: Register in ModuleLoader.java
Step 5: Add navigation in DashboardController
```

### 3. Custom Validation Rules
```
Step 1: Extend Validator class
Step 2: Add validation method
Step 3: Update FlashingService
Step 4: Display results in UI
```

---

## Quality Characteristics

| Attribute | Implementation |
|-----------|----------------|
| **Reliability** | Async ops, error handling, session persistence, auto-reconnect |
| **Usability** | Intuitive FXML UI, real-time feedback, theme customization |
| **Maintainability** | Layered architecture, clear separation, documented code |
| **Performance** | Async I/O, thread pooling, native optimization |
| **Portability** | Java 20, JavaFX (cross-platform), Windows native layer |

---

## Deployment

```
Distribution: JAR executable (Maven packaging)
Main Class: com.aerospace.aitt.shell.MainApp
Runtime: JDK 20+
OS: Windows 10/11 (x64)
Memory: 512MB (min), 2GB (recommended)
Disk: 200MB
External: TRACE32 (optional, path configured in settings)
```

---

## Technologies Used

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 20 |
| **UI Framework** | JavaFX | 20.0.2 |
| **Build Tool** | Maven | 3.x |
| **JSON** | Jackson | 2.16.1 |
| **Logging** | Logback + SLF4J | 1.4.14 |
| **Testing** | JUnit | 5.10.2 |
| **Native Build** | CMake | 3.16+ |
| **Native Compiler** | Visual Studio | 2019/2022 |
| **Native Language** | C | C11 |

