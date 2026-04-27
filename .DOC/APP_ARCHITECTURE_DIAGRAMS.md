# AITT Architecture Diagram - Visual Components

## 1. Flashing Module - Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLASHING MODULE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           UI LAYER (Presentation)                        │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │                                                          │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │ FlashingController (JavaFX Controller)          │   │  │
│  │  │ ─ Firmware module table                         │   │  │
│  │  │ ─ Target profile selector                       │   │  │
│  │  │ ─ Action buttons (Validate, Generate, Run)     │   │  │
│  │  │ ─ Progress bar + status label                  │   │  │
│  │  │ ─ Log viewer                                    │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │        SERVICE LAYER (Business Logic)                    │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │                                                          │  │
│  │  FlashingService (Main Orchestrator)                    │  │
│  │  ├─ Async workflow management                          │  │
│  │  ├─ Thread pool executor (4 workers)                   │  │
│  │  ├─ Progress & abort handling                          │  │
│  │  └─ Phase coordination:                                │  │
│  │     ├─ Phase 1 (10%): Validation                       │  │
│  │     ├─ Phase 2 (30%): CMM Generation                   │  │
│  │     └─ Phase 3 (60%): TRACE32 Execution                │  │
│  │                                                          │  │
│  │  ┌──────────────────┐ ┌──────────────────┐             │  │
│  │  │ Validator        │ │ CmmGenerator     │             │  │
│  │  ├──────────────────┤ ├──────────────────┤             │  │
│  │  │ • File check     │ │ • Script builder │             │  │
│  │  │ • Address valid  │ │ • Header/footer  │             │  │
│  │  │ • Overlap detect │ │ • Module commands│             │  │
│  │  │ • Boundary check │ │ • Verification   │             │  │
│  │  │ • Forbidden zone │ │ • File save      │             │  │
│  │  └──────────────────┘ └──────────────────┘             │  │
│  │  ┌──────────────────────────────────────┐              │  │
│  │  │ Trace32Runner                        │              │  │
│  │  ├──────────────────────────────────────┤              │  │
│  │  │ • Exe path resolution & validation   │              │  │
│  │  │ • Process lifecycle management       │              │  │
│  │  │ • Output capture & logging           │              │  │
│  │  │ • Timeout handling (30 min default)  │              │  │
│  │  │ • Exit code tracking                 │              │  │
│  │  └──────────────────────────────────────┘              │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                          ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         DATA LAYER (Persistence & Models)                │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │                                                          │  │
│  │  SessionStore          ProfileStore                     │  │
│  │  ├─ Save sessions      ├─ Load profiles                 │  │
│  │  ├─ Load history       ├─ Cache targets                 │  │
│  │  └─ Audit trail        └─ Configuration                 │  │
│  │                                                          │  │
│  │  Models:                                                │  │
│  │  ├─ FlashingSession: ID, modules, target, status        │  │
│  │  ├─ SoftwareModule: name, path, address, size           │  │
│  │  ├─ TargetProfile: CPU, flash range, RAM                │  │
│  │  ├─ FlashStatus: VALIDATING → ... → SUCCESS            │  │
│  │  └─ ValidationResult: success, errors[]                 │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Test Module - Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    TEST MODULE                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           UI LAYER (Presentation)                        │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │                                                          │  │
│  │  TestDevController (JavaFX Controller - Placeholder)    │  │
│  │  ├─ initialize()  │ Initialize UI components            │  │
│  │  ├─ onShow()      │ Handle module display               │  │
│  │  └─ handleBack()  │ Navigation to dashboard             │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Status: ⏳ UNDER DEVELOPMENT                                  │
│  Next Steps: Define test cases, execution framework,           │
│              integration with HardwareService                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 3. Configuration Module - Logging Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              CONFIGURATION MODULE (logback.xml)                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              Appender Configuration                       │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ CONSOLE Appender                                   │ │ │
│  │  │ • Real-time terminal output                        │ │ │
│  │  │ • Pattern: HH:mm:ss [thread] level logger - msg    │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │                                                           │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ FILE Appender (Main Log)                            │ │ │
│  │  │ • File: logs/aitt.log                               │ │ │
│  │  │ • Rotation: Daily + max 30 days + 100 MB cap        │ │ │
│  │  │ • Pattern: yyyy-MM-dd HH:mm:ss [thread] level ...  │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │                                                           │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │ FLASHING Appender (Module-Specific)                 │ │ │
│  │  │ • File: logs/flashing.log                           │ │ │
│  │  │ • Rotation: Daily + max 90 days + 500 MB cap        │ │ │
│  │  │ • Only for: com.aerospace.aitt.flashing.*           │ │ │
│  │  │ • Pattern: yyyy-MM-dd HH:mm:ss [thread] level ...  │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │            Logger Configuration                          │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  Application Loggers:                                   │ │
│  │  ├─ com.aerospace.aitt          → DEBUG level           │ │
│  │  └─ com.aerospace.aitt.flashing → DEBUG + FLASHING app  │ │
│  │                                                           │ │
│  │  Suppressed Loggers:                                    │ │
│  │  ├─ org.sqlite                  → WARN level            │ │
│  │  └─ javafx                      → WARN level            │ │
│  │                                                           │ │
│  │  Root Logger:                                           │ │
│  │  └─ Level: INFO, Appenders: [CONSOLE, FILE]             │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │           Log File Output Structure                       │ │
│  ├───────────────────────────────────────────────────────────┤ │
│  │                                                           │ │
│  │  logs/ (directory)                                      │ │
│  │  │                                                       │ │
│  │  ├─ aitt.log            (current general logs)          │ │
│  │  ├─ aitt.2026-04-05.log (daily rotations)               │ │
│  │  ├─ aitt.2026-04-04.log │ (max 30 days)                 │ │
│  │  │ ...                   │ (max 100 MB total)           │ │
│  │  │                                                       │ │
│  │  ├─ flashing.log         (current flashing logs)        │ │
│  │  ├─ flashing.2026-04-05.log (daily rotations)           │ │
│  │  └─ flashing.2026-04-04.log (max 90 days)               │ │
│  │      ...                  (max 500 MB total)             │ │
│  │                                                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 4. Inter-Module Communication Flow

```
┌────────────────────────────────────────────────────────────────┐
│              AITT APPLICATION INTEGRATION                      │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  FlashingController ──────────┐                               │
│         │                     │                               │
│         ├─ UI interactions    │                               │
│         │                     ↓                               │
│         │              FlashingService                        │
│         │                     │                               │
│         ├─ Query profile ──→ ProfileStore                     │
│         │                                                     │
│         ├─ Save session  ──→ SessionStore                     │
│         │                                                     │
│         └─ Log events ──┐                                     │
│                         ↓                                     │
│  TestDevController ──→ AppLogger (SLF4J)                      │
│         │               │                                     │
│         └─ Logging ────→ Logback Config (logback.xml)         │
│                         │                                     │
│                         ├─ CONSOLE Appender                   │
│                         ├─ FILE Appender → logs/aitt.log      │
│                         └─ FLASHING Appender → logs/flashing.log
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## 5. Flashing Module - Phase Execution Timeline

```
Timeline: User clicks "Execute Flash" button
│
├─ T+0s   ┌─────────────────────────────────┐
│         │ Phase 1: VALIDATING (10%)       │
│         │ ┌───────────────────────────────┤
│         │ │ • Check file exists           │
│         │ │ • Validate addresses          │
│         │ │ • Detect overlaps             │
│         │ │ • Check boundaries            │
│         └─────────────────────────────────┘
│         Progress: 0% → 10%
│
├─ T+Xs  ┌─────────────────────────────────┐
│        │ Phase 2: GENERATING (30%)        │
│        │ ┌───────────────────────────────┤
│        │ │ • Build CMM script header      │
│        │ │ • System initialization       │
│        │ │ • Flash configuration         │
│        │ │ • Module flash commands       │
│        │ │ • Verification section        │
│        │ │ • Save to flash_TIMESTAMP.cmm │
│        └─────────────────────────────────┘
│        Progress: 10% → 40%
│
├─ T+Ys ┌─────────────────────────────────┐
│       │ Phase 3: EXECUTING (60%)         │
│       │ ┌───────────────────────────────┤
│       │ │ • Find TRACE32 executable      │
│       │ │ • Launch t32marm.exe           │
│       │ │ • Load CMM script              │
│       │ │ • Execute on target            │
│       │ │ • Capture output               │
│       │ │ • Timeout: 30 minutes          │
│       └─────────────────────────────────┘
│       Progress: 40% → 100%
│
└─ T+Zs  Status: SUCCESS / FAILURE
         Session saved
         Logs written to logs/flashing.log
```

## 6. Data Model Relationships

```
FlashingSession (Root Entity)
│
├─ id: UUID
├─ timestamp: LocalDateTime
├─ modules[]: SoftwareModule[]  ─→ ┌─────────────────────┐
│                                  │ SoftwareModule      │
│                                  ├─────────────────────┤
│                                  │ name: String        │
│                                  │ filePath: Path      │
│                                  │ address: long       │
│                                  │ size: long          │
│                                  │ checksum: String    │
│                                  └─────────────────────┘
│
├─ targetProfile: TargetProfile  ─→ ┌─────────────────────┐
│                                    │ TargetProfile      │
│                                    ├─────────────────────┤
│                                    │ name: String        │
│                                    │ cpuType: String     │
│                                    │ flashStart: long    │
│                                    │ flashEnd: long      │
│                                    │ ramAddress: long    │
│                                    │ protocol: String    │
│                                    └─────────────────────┘
│
├─ status: FlashStatus  ─────────→ Enum: VALIDATING, GENERATING,
│                                         EXECUTING, SUCCESS, FAILURE
│                                         
├─ validation: ValidationResult  ─→ ┌──────────────────────────────┐
│                                    │ ValidationResult            │
│                                    ├──────────────────────────────┤
│                                    │ success: boolean            │
│                                    │ errors[]: ValidationError[] │
│                                    └──────────────────────────────┘
│                                           ↓
│                                    ┌──────────────────────────────┐
│                                    │ ValidationError             │
│                                    ├──────────────────────────────┤
│                                    │ type: ErrorType             │
│                                    │ message: String             │
│                                    │ affectedModule: String      │
│                                    └──────────────────────────────┘
│
├─ outputPath: Path
└─ errorMessage: String (if FAILURE)
```

---

## 7. Flashing Module Validation Rules

```
Validation Rules Applied:
├─ File Existence
│  └─ Each module's file must exist and be readable
│
├─ Address Validity
│  ├─ Address must be within target profile flash range
│  └─ Address must not be in forbidden regions (e.g., 0x00000000-0x00001000)
│
├─ Overlap Detection
│  └─ No two modules can occupy the same memory address range
│
├─ Boundary Checking
│  ├─ Module address + size must not exceed flash end address
│  └─ Module must not start before flash start address
│
└─ Duplicate Address Detection
   └─ No two modules can have the same starting address

Forbidden Regions (Hard-coded):
├─ 0x00000000 - 0x00001000 (Null pointer region, 4 KB reserved)
└─ Additional regions may be defined per TargetProfile
```

---

## Summary: Three-Module Architecture

| Module | Type | Status | Key Components | Purpose |
|--------|------|--------|-----------------|---------|
| **Flashing** | Service | ✅ Active | Controller, Service, Validator, CmmGenerator, Trace32Runner, Stores | Firmware flashing orchestration |
| **Test** | Framework | ⏳ Dev | Controller (placeholder) | Hardware testing framework (future) |
| **Configuration** | System | ✅ Active | logback.xml, Appenders, Loggers | Centralized logging & settings |
