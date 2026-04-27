# AITT Application Architecture - Focused View
## Flashing, Test, and Configuration Modules Only

---

## Overview

This document describes the architecture of the AITT (Automation Integration Test Tool) application focusing on three core modules:
1. **Flashing Module** - Firmware flashing orchestration and execution
2. **Test Module** - Hardware-level testing framework (in development)
3. **Configuration Module** - Application-wide logging and settings management

---

## Module 1: Flashing Module

### Purpose
Manages the complete lifecycle of firmware flashing operations, from validation through TRACE32 execution on automotive MCU targets.

### Architecture Layers

#### **Presentation Layer (UI)**
- **FlashingController** (FXML Controller)
  - Manages JavaFX UI interactions
  - Firmware module table display and management
  - Target profile selection
  - Progress tracking and user feedback
  - Key responsibilities:
    - Add/remove firmware modules
    - Validate modules
    - Generate CMM scripts
    - Execute TRACE32 debugger
    - Display real-time progress and logs

#### **Service Layer (Orchestration)**
- **FlashingService** (Main Orchestrator)
  - Coordinates the complete flashing workflow
  - Manages async execution with `CompletableFuture`
  - Thread pool execution (`FixedThreadPool` with 4 workers)
  - Abort and cancellation handling
  - Three-phase execution:
    1. **Validation Phase (10%)** - Module integrity check
    2. **CMM Generation Phase (30%)** - Script creation
    3. **TRACE32 Execution Phase (60%)** - Hardware flashing

#### **Utility Services**
- **Validator**
  - Validates firmware modules against target profile
  - Checks:
    - File existence and readability
    - Memory address validity
    - Address overlap detection
    - Boundary violations
    - Duplicate address detection
    - Forbidden region protection (null pointer region: 0x00000000-0x00001000)
  - Returns: `ValidationResult` with detailed errors

- **CmmGenerator**
  - Generates TRACE32 CMM (command macro) scripts
  - Constructs script sections:
    - Header with metadata (timestamp, target, session ID)
    - System initialization
    - Flash configuration
    - Per-module flash commands
    - Verification section
  - Saves scripts to disk with timestamped filenames

- **Trace32Runner**
  - Executes TRACE32 debugging tool
  - Process lifecycle management
  - Command line: `t32marm.exe`
  - Features:
    - Executable path resolution (T32SYS env var or default)
    - Process output capture
    - Timeout handling (default 30 minutes)
    - Graceful process termination
    - Exit code tracking

#### **Data Layer**
- **SessionStore**
  - Persist flashing sessions for history and recovery
  
- **ProfileStore**
  - Manages target chipset profiles
  - Provides target metadata (CPU type, flash range, RAM address)

#### **Data Models**
- **SoftwareModule**
  - Name, file path, flash address, size
  - Checksum for verification
  
- **TargetProfile**
  - CPU type (S32K148, MPC5777M)
  - Flash memory range
  - RAM address space
  - Protocol settings (SWD/JTAG)

- **FlashingSession**
  - Unique session ID
  - Module list
  - Target profile reference
  - Status tracking
  - Timestamp and output path
  - Error logging
  
- **FlashStatus** (Enum)
  - VALIDATING → GENERATING → EXECUTING → SUCCESS/FAILURE
  
- **ValidationResult**
  - Success/failure flag
  - Error list with types and module references
  
- **ValidationErrorType** (Enum)
  - FILE_NOT_FOUND
  - INVALID_ADDRESS
  - OVERLAP_DETECTED
  - BOUNDARY_EXCEEDED
  - DUPLICATE_ADDRESS

### Execution Flow

```
User Interaction (FlashingController UI)
         ↓
    Add Modules
         ↓
    Select Target Profile
         ↓
    Validate Modules ← Validator
         │
         ├─ Check files exist
         ├─ Check addresses valid
         └─ Detect overlaps
         ↓
    Generate CMM Script ← CmmGenerator
         │
         ├─ System init commands
         ├─ Flash config
         └─ Module flash commands
         ↓
    Execute TRACE32 ← Trace32Runner
         │
         ├─ Find executable
         ├─ Load config
         └─ Run script
         ↓
    Display Results to UI ← FlashingService
```

---

## Module 2: Test Module

### Purpose
Framework for hardware-level testing and validation (currently in development).

### Current State
- **TestDevController** - Placeholder UI component
- Minimal implementation, awaiting requirements finalization
- Navigation-only functionality
- Ready for expansion

### Structure
```
testdev/
└── ui/
    └── TestDevController
        ├── initialize() - Setup
        ├── onShow() - Display handling
        └── handleBack() - Navigation
```

### Future Considerations
- Integration with HardwareService for hardware communication
- Test case definition and execution
- Result reporting and analysis
- Parallel execution with other modules

---

## Module 3: Configuration Module

### Logging Configuration (logback.xml)

#### **Appenders**

1. **CONSOLE Appender**
   - Real-time terminal output
   - Pattern: `%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
   - All messages passed through

2. **FILE Appender** (Main Application Log)
   - File: `logs/aitt.log`
   - Rolling policy: Time-based daily rotation
   - Max history: 30 days
   - Total size cap: 100 MB
   - Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n`

3. **FLASHING Appender** (Module-Specific Log)
   - File: `logs/flashing.log`
   - Rolling policy: Time-based daily rotation
   - Max history: 90 days
   - Total size cap: 500 MB
   - Dedicated to `com.aerospace.aitt.flashing` package
   - Separate from general application logs

#### **Logger Configuration**

| Logger Name | Level | Appenders | Purpose |
|-------------|-------|-----------|---------|
| `com.aerospace.aitt` | DEBUG | CONSOLE, FILE | All application logs |
| `com.aerospace.aitt.flashing` | DEBUG | FLASHING (+ parent) | Flashing module operations |
| `org.sqlite` | WARN | (inherited) | Suppress SQLite verbosity |
| `javafx` | WARN | (inherited) | Suppress JavaFX framework logs |
| Root | INFO | CONSOLE, FILE | Fallback for all other logs |

#### **Log Directory Structure**
```
logs/
├── aitt.log                    (general application logs)
├── aitt.2026-04-05.log        (daily rotation)
├── aitt.2026-04-04.log
├── flashing.log               (flashing-specific logs)
├── flashing.2026-04-05.log   (daily rotation with 90-day history)
└── flashing.2026-04-04.log
```

#### **Key Logging Characteristics**
- **Thread-aware**: All logs include thread name
- **Level filtering**: DEBUG for AITT modules, WARN for external libraries
- **Capacity management**: Automatic cleanup after retention limits
- **Performance**: Asynchronous rolling file appender

---

## Inter-Module Communication

### Flashing to Configuration
```
FlashingController/FlashingService
         ↓
    AppLogger (uses logback.xml)
         ↓
    logs/flashing.log (configured appender)
```

### Module Dependencies

```
FlashingController
    ├─ FlashingService
    │   ├─ Validator
    │   ├─ CmmGenerator
    │   ├─ Trace32Runner
    │   ├─ SessionStore
    │   └─ ProfileStore
    ├─ ProfileStore
    ├─ SessionStore
    └─ AppLogger

TestDevController
    └─ AppLogger

[All modules]
    └─ logback.xml (Logging Configuration)
```

---

## Data Flow Diagram - Flashing Module

```
┌─────────────────────┐
│ FlashingController  │ (UI)
└──────────┬──────────┘
           │
           ├─ getFirmwareModules() ────────→ moduleRows (ObservableList)
           │
           ├─ getSelectedProfile() ────────→ ProfileStore
           │
           ├─ validateAsync() ─────────────→ FlashingService
           │       │                               │
           │       │                    ┌──────────┴─────────┐
           │       │                    │                    │
           │       └──────────────────→ Validator ←─────────┘
           │                               │
           │                               ├─ validateModule()
           │                               ├─ checkOverlaps()
           │                               └─ checkDuplicateAddresses()
           │
           └─ executeAsync() ──────────────→ FlashingService
                       │                            │
                ┌──────┴──────────────────────────┬─┘
                │                                 │
           Phase 1: Validate         Executor.supplyAsync()
                │
           Phase 2: Generate CMM ────→ CmmGenerator
                │                            │
                │                   ┌────────┴────────┐
                │                   │                 │
                │            generate()      saveTo()
                │
           Phase 3: Execute T32 ─────→ Trace32Runner
                │                            │
                │                   ┌────────┴────────┐
                │                   │                 │
                │            run()        findT32Exe()
                │
                └─(Progress Callbacks)─→ ProgressBar, lblStatus
```

---

## Key Design Patterns

### 1. **Orchestrator Pattern** (FlashingService)
- Centralized coordination of multi-step process
- Clear separation of concerns with Service > Util Services

### 2. **Async Execution Pattern**
- `CompletableFuture` for non-blocking operations
- Thread pool executor for concurrent operations
- Progress callbacks for real-time UI updates

### 3. **Validation Pattern**
- Validator returns rich `ValidationResult` object
- Multiple validation checks (overlap, boundaries, duplicates)
- Error accumulation and detailed reporting

### 4. **Data Persistence Pattern**
- SessionStore and ProfileStore for state management
- Flashing sessions saved for recovery and audit

### 5. **Configuration Pattern**
- Logback for centralized logging management
- Module-specific appenders for focused analysis
- Log rotation for capacity management

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| **UI Framework** | JavaFX |
| **Logging** | Logback + SLF4J |
| **Threading** | Java Concurrent Framework |
| **Script Generation** | StringBuilder templating |
| **External Tool** | TRACE32 debugger (t32marm.exe) |
| **Data Storage** | Session/Profile stores (persistence layer) |
| **File Operations** | Java NIO Paths, FileUtil |

---

## Application Flow - From Startup to Flashing

```
1. MainApp.start() → DashboardController
2. User navigates to Flashing module → FlashingController.onShow()
3. User adds firmware modules → moduleRows updated
4. User selects target profile → profileComboBox selection
5. User clicks "Validate" → validateAsync()
   → Validator.validate() → ValidationResult
6. User clicks "Generate CMM" → execute()
   → Phase 1: Validator.validate()
   → Phase 2: CmmGenerator.generate() → saves to flash_TIMESTAMP.cmm
   → Phase 3: Trace32Runner.run()
7. Progress callbacks update UI → ProgressBar, lblStatus
8. Results logged to logs/flashing.log (via logbackl.xml appender)
9. Session persisted → SessionStore
```

---

## Error Handling Strategy

### Validation Errors
- Caught and accumulated in `ValidationResult`
- User feedback via alerts
- Session logged with error details

### Process Errors (CMM/TRACE32)
- Exceptions caught in FlashingService
- Error message stored in FlashingSession
- UI notified via progress callbacks
- Full stack trace in logs/flashing.log

### Threading Errors
- Executor uncaught exception handler configured
- Thread-safe abort mechanism
- Current session preserved for recovery

---

## Summary Table

| Aspect | Details |
|--------|---------|
| **Primary Purpose** | Automate firmware flashing for automotive MCUs |
| **Processing Model** | Async, multi-phase orchestration |
| **Concurrency** | Fixed thread pool (4 workers) |
| **UI Framework** | JavaFX with observable data binding |
| **Validation Scope** | Files, addresses, overlaps, boundaries |
| **External Tools** | TRACE32 (t32marm.exe) |
| **Logging** | Logback (general + module-specific) |
| **Persistence** | SessionStore, ProfileStore |
| **State Management** | FlashingSession with full audit trail |
