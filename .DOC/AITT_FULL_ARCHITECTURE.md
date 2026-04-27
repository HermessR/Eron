# AITT (Automation Integration Test Tool) - Complete Architecture Documentation

**Version:** 1.0.0  
**Last Updated:** April 17, 2026  
**Purpose:** Complete codebase architecture for integration with CMM generation systems

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [System Architecture](#system-architecture)
4. [Package Structure](#package-structure)
5. [Core Components](#core-components)
6. [Data Models](#data-models)
7. [Service Layer](#service-layer)
8. [CMM Generation Architecture](#cmm-generation-architecture)
9. [Data Flow & Workflows](#data-flow--workflows)
10. [Configuration & Persistence](#configuration--persistence)
11. [Native Layer Integration](#native-layer-integration)
12. [Design Patterns](#design-patterns)
13. [Integration Points](#integration-points)

---

## Project Overview

**AITT** is a JavaFX desktop application for integration testing of aeronautical embedded systems. It provides comprehensive firmware flashing, hardware debugging, and remote probe management capabilities.

### Key Features
- **Firmware Flashing**: Load and verify firmware binaries on embedded systems
- **Hardware Management**: Detect and manage USB/Serial debug probes
- **Remote Debugging**: Support for network-attached debug probes via TCP/mDNS
- **CMM Script Generation**: Automated TRACE32 CMM script creation for embedded systems
- **Multi-Chipset Support**: MPC5777M, S32K148, and extensible architecture
- **Session Management**: Persist and recover flashing sessions
- **User Authentication**: Session-based user management (simple auth, future: license system)

---

## Technology Stack

### Frontend Framework
- **JavaFX 20.0.2**: Desktop UI framework
- **FXML**: XML-based UI markup language
- **Custom Components**: InfiniteCarousel, ThemeManager

### Backend & Serialization
- **Jackson 2.16.1**: JSON serialization/deserialization
- **Jackson DataType JSR310**: Java 8+ date/time support

### Build System
- **Maven 3.9+**: Project build automation
- **JDK 20**: Java compiler target
- **JNI (Java Native Interface)**: C/C++ integration

### Logging & Utilities
- **SLF4J 2.0.12**: Logging facade
- **Logback 1.4.14**: Logging implementation
- **JUnit 5.10.2**: Testing framework
- **Mockito 5.10.0**: Mocking library

### Native Libraries
- **C/C++**: Hardware bridge implementation (aitt_native.dll/so)
- **CMake 3.27+**: Native build system
- **Visual Studio 2022** (Windows): C++ compiler

---

## System Architecture

### High-Level Layering

```
┌─────────────────────────────────────────────┐
│         PRESENTATION LAYER (JavaFX)         │
│  Controllers: Dashboard, Flashing, Hardware │
│  Views: FXML UI components and layouts      │
└─────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────┐
│    CORE INFRASTRUCTURE & UI UTILITIES       │
│  StageManager, ThemeManager, BaseController │
│  Authentication, Configuration, Logging     │
└─────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────┐
│      APPLICATION SERVICES LAYER             │
│  FlashingService, NetworkProbeService       │
│  HardwareService, AuthService               │
│  Orchestrates workflows and business logic  │
└─────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────┐
│    BUSINESS LOGIC & DATA MODELS             │
│  FlashingSession, SoftwareModule            │
│  TargetProfile, NetworkProbe                │
│  ValidationResult, CommandResult            │
└─────────────────────────────────────────────┘
                    ↓ depends on
┌─────────────────────────────────────────────┐
│      BACKEND HARDWARE & UTILITIES           │
│  HardwareAbstractionLayer, Validators       │
│  CMMScriptGenerator, Trace32Runner          │
│  FileUtil, ChecksumUtil, HexUtil            │
└─────────────────────────────────────────────┘
                    ↓ JNI bridge
┌─────────────────────────────────────────────┐
│       NATIVE LAYER (C/C++)                  │
│  HardwareBridge JNI wrapper                 │
│  Native core: probe detection, protocol     │
│  Chipset drivers: MPC5777M, S32K148         │
└─────────────────────────────────────────────┘
                    ↓ USB/Serial/Ethernet
┌─────────────────────────────────────────────┐
│    EXTERNAL HARDWARE & SYSTEMS              │
│  USB Debug Probes, Serial Ports             │
│  TRACE32 Execution Environment              │
│  Network-attached debug hardware            │
└─────────────────────────────────────────────┘
```

---

## Package Structure

### Root Package: `com.aerospace.aitt`

```
aitt/
├── src/main/java/com/aerospace/aitt/
│   ├── App.java                          # JavaFX launcher wrapper
│   ├── shell/                            # Application shell & navigation
│   │   ├── MainApp.java                  # Primary application entry
│   │   ├── LoginController.java          # Authentication UI
│   │   ├── DashboardController.java      # Main dashboard with carousel
│   │   └── ModuleLoader.java             # Module registry
│   │
│   ├── core/                             # Core infrastructure
│   │   ├── auth/
│   │   │   ├── AuthService.java          # Authentication & session management (Singleton)
│   │   │   └── User.java                 # User model (role, login timestamp)
│   │   │
│   │   ├── config/
│   │   │   ├── AppSettings.java          # Configuration data model
│   │   │   ├── SettingsStore.java        # Persistence layer (Singleton)
│   │   │   └── ui/SettingsController.java # Settings UI
│   │   │
│   │   ├── ui/
│   │   │   ├── BaseController.java       # Abstract base for all controllers
│   │   │   ├── StageManager.java         # Scene/stage navigation (Singleton)
│   │   │   ├── ThemeManager.java         # Dark/light theme management
│   │   │   ├── Alerts.java               # Alert dialog utilities
│   │   │   └── InfiniteCarousel.java     # Custom carousel component
│   │   │
│   │   ├── util/
│   │   │   ├── FileUtil.java             # File operations
│   │   │   ├── ChecksumUtil.java         # CRC32 checksum calculation
│   │   │   └── HexUtil.java              # Hexadecimal conversions
│   │   │
│   │   └── log/
│   │       └── AppLogger.java            # Application logging facade
│   │
│   ├── flashing/                         # Firmware flashing module
│   │   ├── model/
│   │   │   ├── FlashingSession.java      # Session container (Record)
│   │   │   ├── SoftwareModule.java       # Firmware binary (Record)
│   │   │   ├── TargetProfile.java        # Chipset profile (Record)
│   │   │   ├── FlashStatus.java          # Status enum
│   │   │   └── ValidationResult.java     # Validation result container
│   │   │
│   │   ├── service/
│   │   │   ├── FlashingService.java      # Main orchestrator service
│   │   │   ├── Validator.java            # Module validation logic
│   │   │   ├── CmmGenerator.java         # TRACE32 CMM script generation
│   │   │   └── Trace32Runner.java        # TRACE32 process execution
│   │   │
│   │   ├── data/
│   │   │   ├── SessionStore.java         # Session persistence
│   │   │   └── ProfileStore.java         # Profile persistence
│   │   │
│   │   └── ui/
│   │       └── FlashingController.java   # Main flashing UI
│   │
│   ├── remote/                           # Remote probe management module
│   │   ├── model/
│   │   │   └── NetworkProbe.java         # Network probe representation (Record)
│   │   │
│   │   ├── service/
│   │   │   ├── NetworkProbeService.java  # Probe discovery & management
│   │   │   └── RemoteFlashingService.java # Remote execution
│   │   │
│   │   └── ui/
│   │       └── RemoteProbeController.java # Probe management UI
│   │
│   ├── native_/                          # Native hardware bridge (JNI)
│   │   ├── HardwareBridge.java           # JNI bridge (Singleton)
│   │   ├── HardwareService.java          # Service wrapper
│   │   ├── ChipsetConfig.java            # Chipset constants
│   │   ├── HardwareInfo.java             # Device information
│   │   ├── CommandResult.java            # Native command results
│   │   └── NativeException.java          # Native error representation
│   │
│   ├── backend/                          # Backend hardware abstraction
│   │   └── hardware/
│   │       ├── HardwareAbstractionLayer.java # Chipset-agnostic interface
│   │       ├── MPCHardwareImpl.java           # MPC5777M implementation
│   │       └── CMMScriptGenerator.java       # Backend CMM generation
│   │
│   ├── hardware/                         # Hardware discovery module
│   │   └── ui/
│   │       └── HardwareController.java   # Hardware probe UI
│   │
│   ├── config/                           # Configuration module
│   │   └── ui/
│   │       └── ConfigController.java     # Configuration UI
│   │
│   ├── scripting/                        # Script development module
│   │   └── ui/
│   │       └── ScriptingController.java  # Script UI
│   │
│   └── testdev/                          # Test development module
│       ├── service/
│       │   └── CMMTestService.java       # Test case generation
│       └── ui/
│           └── TestDevController.java    # Test development UI
│
├── src/main/resources/
│   ├── logback.xml                       # Logging configuration
│   ├── css/
│   │   ├── style.css                     # Default theme
│   │   ├── style-light.css               # Light theme
│   │   └── carousel-demo.css             # Carousel styling
│   ├── fxml/
│   │   ├── login.fxml                    # Login screen
│   │   ├── dashboard.fxml                # Main dashboard
│   │   ├── flashing.fxml                 # Flashing UI
│   │   ├── hardware.fxml                 # Hardware UI
│   │   ├── remote.fxml                   # Remote probe UI
│   │   ├── settings.fxml                 # Settings UI
│   │   ├── config.fxml                   # Configuration UI
│   │   └── scripting.fxml                # Scripting UI
│   ├── native/                           # Native resources
│   ├── profiles/                         # Target profile JSONs
│   └── sounds/                           # Audio resources
│
├── native/                               # C/C++ source code
│   ├── CMakeLists.txt                    # CMake build configuration
│   ├── build/                            # CMake build output
│   ├── include/
│   │   ├── aitt_native.h                 # C header
│   │   └── generated/                    # JNI generated headers
│   └── src/
│       ├── aitt_native.c                 # Core native implementation
│       ├── jni_bindings.c                # JNI method bindings
│       ├── chipset_mpc5777m.c            # MPC5777M driver
│       └── chipset_s32k148.c             # S32K148 driver
│
├── pom.xml                               # Maven configuration
└── target/                               # Build output
```

---

## Core Components

### 1. Application Shell (`com.aerospace.aitt.shell`)

#### MainApp.java (Application Entry)
```
Responsibilities:
  • JavaFX Application lifecycle management
  • Initialization of core services (StageManager, AuthService)
  • Native library pre-warming
  • Login screen display on startup
  
Key Methods:
  • start(Stage primaryStage) - Application entry point
  • initializeServices() - Service initialization
  • ensureNativeLibraryLoaded() - JNI library loading
```

#### LoginController.java
```
Responsibilities:
  • User authentication UI
  • Credential validation via AuthService
  • Navigation to dashboard on success
  
Key Features:
  • Username/password input fields
  • Remember login option
  • Error message display
```

#### DashboardController.java
```
Responsibilities:
  • Main application dashboard
  • Module carousel navigation
  • User information display
  • Session management
  
Key Features:
  • Infinite carousel with module tiles
  • Quick action buttons
  • User profile info
  • System status display
```

#### ModuleLoader.java
```
Responsibilities:
  • Module registry and discovery
  • Dynamic module loading
  • Module lifecycle management
  
Key Methods:
  • registerModule(String name, Class<?> moduleClass)
  • loadModule(String name)
  • getAvailableModules()
```

---

### 2. Core Infrastructure (`com.aerospace.aitt.core.*`)

#### Authentication (`core.auth`)

**AuthService (Singleton)**
```
Responsibilities:
  • User session management
  • Credential validation
  • Session persistence
  
Key Methods:
  • authenticate(String username, String password): User
  • logout()
  • getCurrentUser(): Optional<User>
  • isAuthenticated(): boolean

Note: Currently uses simple validation. Will be replaced with license system.
```

**User (Record)**
```
Fields:
  • id: String
  • username: String
  • role: String
  • loginTimestamp: Instant
```

#### Configuration (`core.config`)

**AppSettings (Data Model)**
```
Manages:
  • TRACE32 Settings
    - trace32ExecutablePath: String
    - connectionTimeout: int (seconds)
  
  • Flashing Settings
    - defaultTargetProfile: String
    - autoValidate: boolean
    - confirmBeforeFlash: boolean
    - confirmBeforeAbort: boolean
  
  • Output Settings
    - defaultOutputDirectory: String
    - autoGenerateFilename: boolean
    - openOutputFolderAfterGenerate: boolean
  
  • Appearance Settings
    - theme: String (dark/light)
    - logMaxLines: int
    - showTimestampInLog: boolean
    - wrapLogText: boolean

JSON Serializable: Yes (Jackson)
```

**SettingsStore (Singleton)**
```
Responsibilities:
  • Persist settings to ~/.aitt/settings.json
  • Load settings on application startup
  • Provide default settings
  
Key Methods:
  • save(AppSettings settings): void
  • load(): AppSettings
  • getDefaults(): AppSettings
```

#### UI Utilities (`core.ui`)

**BaseController (Abstract)**
```
Base class for all FXML controllers

Provides:
  • UI threading utilities (runOnUiThread)
  • Alert/dialog helpers
  • Async task execution
  • Stage reference management
  
Lifecycle Methods:
  • initialize() - Called after FXML loading
  • onShow() - Called when view is displayed
  • onHide() - Called when view is hidden
  • onClose() - Called on cleanup
```

**StageManager (Singleton)**
```
Responsibilities:
  • Central scene and stage management
  • FXML loading and caching
  • Screen navigation
  
Key Methods:
  • navigateTo(String screenName)
  • loadFXML(String fxmlFile): Parent
  • setMainStage(Stage stage)
  • switchScene(Parent root)
  
Supported Screens:
  • login, dashboard, flashing, hardware, remote
  • settings, config, scripting, testdev
```

**ThemeManager**
```
Responsibilities:
  • Dark/light theme switching
  • CSS stylesheet management
  • Theme persistence
  
Supported Themes:
  • dark (default): style.css
  • light: style-light.css
  
Key Methods:
  • setTheme(String theme)
  • getCurrentTheme(): String
  • applyTheme(Scene scene)
```

**Alerts**
```
Dialog utilities for common alert patterns

Methods:
  • showInfo(String title, String content)
  • showWarning(String title, String content)
  • showError(String title, String content)
  • showConfirmation(String title, String content): boolean
  • showInputDialog(String title, String prompt): Optional<String>
```

#### Utilities (`core.util`)

**FileUtil**
```
Static utility methods:
  • isValidFile(Path path): boolean
  • getBaseName(Path path): String
  • getFileExtension(Path path): String
  • getFileSize(Path path): long
  • formatFileSize(long bytes): String
  • writeString(Path path, String content): void
  • readString(Path path): String
```

**ChecksumUtil**
```
Checksum calculation utilities:
  • crc32Hex(Path file): String
  • crc32(Path file): long
  • md5Hex(Path file): String
  • sha256Hex(Path file): String
```

**HexUtil**
```
Hexadecimal conversion:
  • toHex(long value): String
  • toHex(byte[] bytes): String
  • parseHex(String hex): long
  • formatAddress(long address): String (0x12345678)
```

#### Logging (`core.log`)

**AppLogger (SLF4J Wrapper)**
```
Usage:
  private static final AppLogger log = new AppLogger(MyClass.class);
  log.info("Message");
  log.debug("Debug info");
  log.warn("Warning: {}", variable);
  log.error("Error:", exception);

Configuration: logback.xml
  • Default level: INFO
  • Outputs to: console + rolling file (logs/aitt.log)
```

---

### 3. Flashing Module (`com.aerospace.aitt.flashing.*`)

#### Models (`flashing.model`)

**FlashingSession (Record - Immutable)**
```
Represents an immutable flashing operation session

Fields:
  • id: UUID                           # Unique session identifier
  • modules: List<SoftwareModule>      # Firmware binaries to flash
  • targetProfile: TargetProfile       # Target chipset configuration
  • outputScriptPath: Path             # Generated CMM script location
  • timestamp: Instant                 # Session creation time
  • status: FlashStatus                # Current operation status
  • errorMessage: String               # Error details if failed
  • logs: List<String>                 # Operation logs
  • remoteProbeId: String              # Optional remote probe identifier

Status Values:
  • PENDING - Not yet started
  • VALIDATING - Validating modules
  • GENERATING - Creating CMM script
  • EXECUTING - Running TRACE32
  • COMPLETED - Successfully finished
  • FAILED - Operation failed
  • ABORTED - User aborted

Builder Methods:
  • withStatus(FlashStatus)
  • withError(String)
  • addLog(String)
  • withOutputPath(Path)

Creation:
  • static create(List<SoftwareModule>, TargetProfile)
  • record constructor with defaults
```

**SoftwareModule (Record - Immutable)**
```
Represents a firmware binary to be flashed

Fields:
  • id: UUID                   # Unique module identifier
  • name: String               # Human-readable name
  • filePath: Path             # Path to binary file
  • flashAddress: long         # Target flash memory address
  • size: long                 # Module size in bytes
  • checksum: String           # CRC32 hex string

Computed Properties:
  • endAddress(): long         # Exclusive end address (flashAddress + size)
  • fileName(): String         # File name from path
  • fileExtension(): String    # File extension (bin, hex, elf)
  • formattedAddress(): String # Formatted address (0x12345678)
  • formattedSize(): String    # Formatted size (1.5 KB, 256 B)

Helper Methods:
  • overlaps(SoftwareModule other): boolean
  • fromFile(Path, long flashAddress): SoftwareModule (static factory)
  • withFlashAddress(long): SoftwareModule (copy with new address)

Validation:
  • Requires non-null ID, name, path
  • Requires non-negative address and size
  • Automatically calculates size and checksum from file
```

**TargetProfile (Record - Immutable)**
```
Represents target chipset configuration (not user-editable)

Fields:
  • id: String                 # Profile identifier
  • name: String               # Human-readable name
  • cpuType: String            # CPU type (MPC5777M, S32K148)
  • flashStart: long           # Flash region start address
  • flashEnd: long             # Flash region end address (exclusive)
  • ramAddress: long           # RAM base address for programming
  • t32ConfigPath: Path        # TRACE32 config file path
  • description: String        # Profile description

Computed Properties:
  • flashSize(): long          # Total flash size (flashEnd - flashStart)
  • formattedFlashSize(): String # Formatted size (e.g., "2.0 MB")

Helper Methods:
  • isAddressInFlash(long addr): boolean
  • isWithinFlashRegion(SoftwareModule mod): boolean

Persistence:
  • Loaded from JSON profiles in resources/profiles/
  • Immutable after creation (no user editing)
```

**FlashStatus (Enum)**
```
Operation status states:
  • PENDING - Initial state
  • VALIDATING - Checking modules
  • GENERATING - Creating CMM
  • EXECUTING - Running TRACE32
  • COMPLETED - Success
  • FAILED - Error occurred
  • ABORTED - User canceled
```

**ValidationResult (Container)**
```
Encapsulates validation outcome

Fields:
  • valid: boolean
  • errors: List<ValidationError>
  • errorSummary(): String

ValidationError:
  • type: ValidationErrorType
  • message: String
  • module: SoftwareModule (optional)

Error Types:
  • FILE_NOT_FOUND
  • FILE_READ_ERROR
  • INVALID_ADDRESS
  • ADDRESS_OUT_OF_RANGE
  • ADDRESS_OVERLAP
  • INSUFFICIENT_MEMORY
  • DUPLICATE_ADDRESS

Factory Methods:
  • success(): ValidationResult
  • failure(ValidationErrorType, String): ValidationResult
  • failure(List<ValidationError>): ValidationResult
```

#### Services (`flashing.service`)

**FlashingService (Main Orchestrator)**
```
Responsibilities:
  • Coordinate validation → CMM generation → TRACE32 execution
  • Manage operation lifecycle asynchronously
  • Handle session persistence
  • Provide progress and logging callbacks

Architecture:
  • Uses ExecutorService with 4-thread pool
  • Thread-safe volatile state management
  • Callbacks for UI updates

Key Methods:
  1. validate(List<SoftwareModule>, TargetProfile): ValidationResult
     - Synchronous validation check
     - Returns validation errors immediately

  2. execute(...): CompletableFuture<FlashingSession>
     - Main async execution method
     - Coordinates all steps with progress callbacks
     - Parameters:
       • modules: List<SoftwareModule>
       • target: TargetProfile
       • outputPath: Path (CMM script output)
       • runTrace32: boolean
       • trace32Path: Path
       • logCallback: Consumer<String>
       • progressCallback: Consumer<Double>
     
     - Execution Steps:
       Step 1 (10%): VALIDATING - Validate modules against profile
       Step 2 (30%): GENERATING - Create CMM script
       Step 3 (60%): EXECUTING - Run TRACE32 with CMM
       Step 4 (100%): COMPLETED/FAILED - Final status
     
     - Progress Ranges:
       0.0-0.1: Validation
       0.1-0.4: CMM generation
       0.4-0.9: TRACE32 execution
       0.9-1.0: Finalization

  3. abort(): void
     - Request abort of current operation
     - Respects confirmBeforeAbort setting

  4. getCurrentSession(): Optional<FlashingSession>
     - Get session currently being processed

Callback Integration:
  • logCallback: Called with status messages
  • progressCallback: Called with 0.0-1.0 values

State Management:
  • abortRequested: volatile boolean
  • currentSession: volatile FlashingSession
  • executor: ExecutorService (4 threads)
```

**Validator**
```
Validates firmware modules before flashing

Key Methods:
  • validate(List<SoftwareModule>, TargetProfile): ValidationResult
    - Validates all modules against target
    - Checks each module individually
    - Checks for overlaps between modules
    - Checks for duplicate addresses
    - Returns aggregated ValidationResult

Validations Performed:
  1. File Existence & Readability
     - Check file path exists
     - Verify file is readable
  
  2. Address Validation
     - Address must be non-negative
     - Address must be within flash region
     - Address cannot be in forbidden regions (e.g., null pointer area)
  
  3. Size Validation
     - Module must not exceed target flash size
     - Module end address must not exceed flash end
  
  4. Overlap Detection
     - Modules must not overlap with each other
     - Detects conflicting address ranges
  
  5. Duplicate Address Check
     - No two modules can start at same address

Forbidden Regions:
  • 0x00000000 - 0x00001000 (first 4KB, typically reserved)
  • Configurable per chipset/target
```

**CmmGenerator**
```
Generates TRACE32 CMM scripts for flashing operations

Key Methods:
  • generate(FlashingSession): String
    - Returns complete CMM script as string
    - Does not save to file

  • saveTo(Path, String): void
    - Saves CMM script to specified file path

  • generateAndSave(FlashingSession, Path): void
    - Combines generation and save operations

CMM Script Structure:
  ```
  ; ============================================
  ; Header (timestamp, target, CPU type)
  ; System Initialization
  ;   SYStem.RESet
  ;   SYStem.CPU <cpuType>
  ;   SYStem.CONFIG.DEBUGPORTTYPE JTAG
  ;   SYStem.Up
  ;   WAIT 100.ms
  ; Flash Configuration
  ;   FLASH.RESet
  ;   FLASH.Create <memory range>
  ;   FLASH.TARGET <ram address> <ram size>
  ; For each module:
  ;   Module Flash (print progress)
  ;   Data.LOAD.<format> "<file>" <address>
  ;   FLASH.ReProgram ALL
  ;   FLASH.ReProgram OFF
  ; Verification section
  ; Footer
  ```

Load Commands (by file extension):
  • .bin: Data.LOAD.Binary "<path>" 0x<address>
  • .hex: Data.LOAD.IntelHEX "<path>"
  • .elf: Data.LOAD.Elf "<path>"

Timestamp Format: "yyyy-MM-dd HH:mm:ss" (system timezone)
Session Tracking: Includes session ID in CMM header
```

**Trace32Runner**
```
Executes TRACE32 process with generated CMM scripts

Key Methods:
  • run(Path cmmScript, Path trace32Executable): CompletableFuture<Boolean>
    - Launches TRACE32 with CMM script
    - Runs asynchronously
    - Returns future with success/failure

  • runAndWait(Path cmmScript, Path trace32Executable, Duration timeout): boolean
    - Synchronous execution with timeout
    - Waits for TRACE32 to complete

Configuration:
  • Command format: "C:\\Program Files\\Trace32\\bin\\t32m.exe -c <cmmScript>"
  • Working directory: CMM script parent directory
  • Error handling: Captures stderr/stdout

Execution:
  • Monitors process completion
  • Handles TRACE32 exit codes
  • Supports process termination on timeout
```

#### Data Persistence (`flashing.data`)

**SessionStore**
```
Persists flashing sessions to disk

Methods:
  • save(FlashingSession): void
    - Saves session as JSON

  • load(UUID sessionId): Optional<FlashingSession>
    - Loads session from disk

  • listAll(): List<FlashingSession>
    - Lists all persisted sessions

Storage:
  • Location: ~/.aitt/sessions/
  • Format: JSON
  • Naming: <sessionId>.json

Session Metadata:
  • Creation timestamp
  • Module information
  • Execution logs
  • Final status
```

**ProfileStore**
```
Manages target profile library

Methods:
  • load(String profileId): Optional<TargetProfile>
    - Loads profile by ID

  • listAll(): List<TargetProfile>
    - Lists available profiles

  • save(TargetProfile): void
    - Persist new profile (for custom profiles)

Built-in Profiles (immutable):
  • mpc5777m - MPC5777M dual-core processor
  • s32k148 - S32K148 single-core ARM Cortex-M4F
  • (extensible architecture for new chipsets)

Storage:
  • Built-in: resources/profiles/ (JSON)
  • Custom: ~/.aitt/profiles/ (JSON)
```

---

### 4. Remote Probe Module (`com.aerospace.aitt.remote.*`)

#### Models (`remote.model`)

**NetworkProbe (Record - Immutable)**
```
Represents a network-attached debug probe

Fields:
  • id: String                 # Unique probe identifier
  • name: String               # Human-readable name
  • host: String               # IP address or hostname
  • port: int                  # TCP port
  • type: RemoteType           # Probe type
  • status: ConnectionStatus   # Current connection status
  • lastSeen: Instant          # Last detection/response time
  • firmware: String           # Firmware version (if available)
  • serialNumber: String       # Hardware serial number

Constants:
  • DEFAULT_T32_PORT = 20000   # TRACE32 default port
  • DEFAULT_AITT_PORT = 20100  # AITT probe server port

Enum: RemoteType
  • T32_POWERDEBUG - TRACE32 PowerDebug with ethernet
  • T32_REMOTE_API - TRACE32 Remote API instance
  • AITT_PROBE_SERVER - AITT Remote Probe Server
  • TCP_DEBUG_BRIDGE - Generic TCP debug bridge
  • UNKNOWN - Unknown type

Enum: ConnectionStatus
  • ONLINE - Probe detected and responsive
  • OFFLINE - Probe not responding
  • UNKNOWN - Status not yet determined
  • ERROR - Connection error occurred
```

#### Services (`remote.service`)

**NetworkProbeService**
```
Discovers and manages network-attached debug probes

Discovery Methods:
  1. UDP Broadcast Discovery
     - Sends discovery packets to local network
     - Listens for probe responses
     - Timeout: configurable (default 5 seconds)

  2. TCP Port Scanning
     - Scans subnet for open debug ports (20000-20100)
     - Configurable address range
     - Timeout per port: 1-2 seconds

  3. mDNS Service Discovery
     - Queries mDNS for _trace32._tcp services
     - Discovers probes advertising via mDNS

Key Methods:
  • startDiscovery(): CompletableFuture<List<NetworkProbe>>
    - Initiates async probe discovery
    - Returns future with discovered probes

  • stopDiscovery(): void
    - Cancels active discovery

  • addListener(Consumer<NetworkProbe>): void
    - Register callback for new probes
    - Called when probe is discovered

  • connect(NetworkProbe probe): CompletableFuture<Boolean>
    - Establish connection to specific probe

  • disconnect(NetworkProbe probe): void
    - Close probe connection

  • getConnectedProbes(): List<NetworkProbe>
    - Return currently connected probes

Async Behavior:
  • Discovery runs in background thread
  • Listeners notified on FX application thread
```

**RemoteFlashingService**
```
Executes flashing operations on remote probes

Key Methods:
  • flashRemote(NetworkProbe probe, FlashingSession session): 
      CompletableFuture<FlashingSession>
    - Execute flashing on remote probe
    - Transfers CMM script and firmware to remote
    - Coordinates remote TRACE32 execution
    - Returns future with completed session

Protocol:
  • Sends CMM script to remote probe via TCP
  • Sends firmware binary references
  • Receives progress updates
  • Handles remote errors

Features:
  • Progress callbacks
  • Timeout handling
  • Error recovery
```

---

### 5. Native Hardware Bridge (`com.aerospace.aitt.native_`)

#### HardwareBridge (JNI Singleton)
```
JNI bridge for low-level hardware communication

Key Methods:
  • getInstance(): HardwareBridge (static, thread-safe)
    - Get singleton instance with lazy initialization

  • detectConnectedHardware(): List<HardwareInfo> (native)
    - Detect USB and Serial debug probes
    - Returns device information (port, vendor ID, product ID)

  • connectProbe(String portName): boolean (native)
    - Establish connection to specific probe
    - Parameterized by port identifier

  • disconnectProbe(): void (native)
    - Close current probe connection

  • sendCommand(String command): CommandResult (native)
    - Send debug command (CMM, JTAG, etc.)
    - Blocking operation
    - Returns result with status and output

  • readMemory(long address, int length): byte[] (native)
    - Read memory from target device
    - Returns raw bytes

  • writeMemory(long address, byte[] data): CommandResult (native)
    - Write memory to target device
    - Returns command result

Native Library:
  • Windows: aitt_native.dll (C++)
  • Linux: libaitt_native.so (C)
  • Loading: Automatic via classloader on first use

Thread Safety:
  • Native implementation uses mutex locks
  • Multiple threads can call simultaneously
  • Single active probe connection per bridge instance

Error Handling:
  • NativeException thrown on native errors
  • CommandResult contains error details
```

#### Supporting Classes

**HardwareInfo**
```
Information about detected hardware device

Fields:
  • id: String                 # Device identifier
  • name: String               # Device name
  • port: String               # Communication port (COM3, /dev/ttyUSB0)
  • vendorId: String           # USB vendor ID
  • productId: String          # USB product ID
  • serialNumber: String       # Device serial number
  • type: HardwareType         # Device classification

HardwareType:
  • USB_JTAG - USB JTAG adapter
  • USB_SERIAL - USB serial port
  • USB_TRACE32 - USB TRACE32 debug probe
  • SERIAL_PORT - Native serial port
  • UNKNOWN
```

**CommandResult**
```
Result of native command execution

Fields:
  • success: boolean           # Command succeeded
  • statusCode: int            # Return code from native layer
  • output: String             # Command output text
  • errorMessage: String       # Error description if failed
  • executionTime: Duration    # Command execution duration
  • dataLength: int            # Length of returned data
```

**ChipsetConfig**
```
Chipset-specific configuration constants

Supports:
  • MPC5777M - Power ISA dual-core, JTAG
  • S32K148 - ARM Cortex-M4F, SWD
  • (Extensible for new chipsets)

Per-chipset settings:
  • Debug protocol type (JTAG, SWD)
  • Memory addresses (flash, RAM)
  • Timing parameters
```

---

### 6. Backend Hardware Abstraction (`com.aerospace.aitt.backend.hardware`)

#### HardwareAbstractionLayer (Interface)
```
Chipset-agnostic hardware address and format resolution

Methods:
  • resolveAddress(String logicalName, String dataType): long
    - Map logical name to physical memory address
    - Example: "temperature_sensor" -> 0x80001000

  • resolveFormat(String logicalName, String dataType): String
    - Get CMM format specifier for data type
    - Example: "int (signed)" -> "sint32"

  • isValidLogicalName(String logicalName): boolean
    - Validate logical name exists in config

  • getChipsetType(): String
    - Return chipset identifier ("MPC5777M", etc.)

Implementations:
  • MPCHardwareImpl - MPC5777M chipset
  • (Other implementations for different chipsets)
```

#### CMMScriptGenerator (Backend)
```
Alternative CMM generation with hardware-aware addressing

Methods:
  • generateScript(TestDefinition testDefinition): Path
    - Generate CMM with hardware-resolved addresses
    - Saves to output directory
    - Returns path to generated script
```

---

## Data Models Summary

### Immutable Records (Thread-safe)
| Model | Purpose | Key Fields |
|-------|---------|-----------|
| `FlashingSession` | Flashing operation state | modules, target, status, logs |
| `SoftwareModule` | Firmware binary | name, filePath, flashAddress, size, checksum |
| `TargetProfile` | Chipset profile | cpuType, flashStart, flashEnd, ramAddress |
| `NetworkProbe` | Remote debug probe | host, port, type, status |
| `User` | Authenticated user | username, role, loginTimestamp |

### Mutable/Data Classes
| Class | Purpose |
|-------|---------|
| `AppSettings` | User configuration (Jackson-serializable) |
| `ValidationResult` | Validation outcome with errors |
| `CommandResult` | Native command execution result |
| `HardwareInfo` | Detected device information |

---

## Service Layer

### Singleton Services
| Service | Scope | Responsibilities |
|---------|-------|-----------------|
| `FlashingService` | Application | Flashing orchestration, CMM generation, TRACE32 execution |
| `NetworkProbeService` | Application | Probe discovery and management |
| `AuthService` | Application | User authentication and session |
| `HardwareBridge` | Application | JNI native hardware access |
| `StageManager` | Application | Scene navigation and FXML loading |
| `ThemeManager` | Application | Theme management |
| `SettingsStore` | Application | Configuration persistence |

### Stateless Services
| Service | Scope | Responsibilities |
|---------|-------|-----------------|
| `Validator` | Module | Firmware validation logic |
| `CmmGenerator` | Module | CMM script generation |
| `Trace32Runner` | Module | TRACE32 process execution |

### Factory/Utility Classes
| Class | Purpose |
|-------|---------|
| `FileUtil` | File operations |
| `ChecksumUtil` | Checksum calculation |
| `HexUtil` | Hexadecimal utilities |
| `AppLogger` | Logging facade |
| `Alerts` | Dialog helpers |
| `BaseController` | Controller base class |

---

## CMM Generation Architecture

### Current Implementation (CmmGenerator.java)

**Generation Process:**
1. **Header Section**
   - Timestamp, target profile, CPU type
   - Session ID for tracking
   - Number of modules

2. **System Initialization**
   ```
   SYStem.RESet
   SYStem.CPU <cpuType>
   SYStem.CONFIG.DEBUGPORTTYPE JTAG
   SYStem.Up
   WAIT 100.ms
   ```

3. **Flash Configuration**
   ```
   FLASH.RESet
   FLASH.Create 1. 0x<start>--0x<end> 0x1000 TARGET Long
   FLASH.TARGET 0x<ramAddr> 0x<ramSize> 0x1000 ~~/demo/arm/flash/long/
   ```

4. **Per-Module Flash Section**
   - Progress printing
   - File loading (binary, hex, or ELF)
   - Flash programming
   - Status reporting

5. **Verification Section**
   - Memory readback verification (optional)
   - Checksum validation

6. **Footer**
   - Completion message
   - Exit sequence

### Integration Points for New CMM Directory

**Extension Areas:**
1. **Hardware-Aware Generation**
   - Resolve logical addresses via HardwareAbstractionLayer
   - Target-specific memory layouts
   - Chipset-specific protocols

2. **Custom Script Templates**
   - Per-chipset CMM templates
   - Custom verification routines
   - Pre/post-flash hooks

3. **Advanced Features**
   - Memory protection configuration
   - ECC handling
   - DMA transfers
   - Sector erase optimization

4. **Test & Validation**
   - Automated verification scripts
   - Memory content validation
   - CRC/checksum verification
   - Register state validation

---

## Data Flow & Workflows

### Workflow 1: Firmware Flashing

```
1. User loads firmware files via UI
   ↓
2. FlashingController receives files
   ↓
3. User selects target profile
   ↓
4. User clicks "Flash"
   ↓
5. FlashingService.execute() called (async)
   ├─ Step 1: Validation (Validator.validate)
   │  ├─ Check file exists and readable
   │  ├─ Verify addresses within flash region
   │  ├─ Check for overlaps and conflicts
   │  └─ Return ValidationResult
   │
   ├─ Progress 10% update → UI
   │
   ├─ Step 2: CMM Generation (CmmGenerator.generate)
   │  ├─ Create header with session info
   │  ├─ Add system init commands
   │  ├─ Add flash configuration
   │  ├─ Add per-module flash sections
   │  ├─ Add verification section
   │  └─ Generate final script
   │
   ├─ Progress 40% update → UI
   │
   ├─ Step 3: TRACE32 Execution (Trace32Runner.run)
   │  ├─ Launch TRACE32 with CMM script
   │  ├─ Monitor process completion
   │  ├─ Capture output/errors
   │  └─ Wait for completion
   │
   ├─ Progress updates 40-90% → UI
   │
   └─ Step 4: Session finalization
      ├─ Update session status
      ├─ Store final logs
      ├─ Persist session
      └─ Return completed session
      
6. UI displays results and logs
```

### Workflow 2: Hardware Discovery

```
1. User opens Hardware panel
   ↓
2. HardwareController calls HardwareBridge.detectConnectedHardware()
   ↓
3. Native bridge enumerates USB/Serial devices
   ↓
4. HardwareInfo objects returned
   ↓
5. HardwareController displays detected devices
   ↓
6. User can connect/disconnect devices via HardwareBridge
```

### Workflow 3: Remote Probe Discovery

```
1. User opens Remote Probe panel
   ↓
2. RemoteProbeController calls NetworkProbeService.startDiscovery()
   ↓
3. NetworkProbeService executes discovery methods:
   ├─ UDP broadcast discovery
   ├─ TCP port scanning
   └─ mDNS service discovery (async)
   ↓
4. Discovered probes → NetworkProbe objects
   ↓
5. Listeners notified for each discovery
   ↓
6. UI displays probe list with status
   ↓
7. User can connect to probe for remote flashing
```

### Workflow 4: Settings Persistence

```
1. User modifies settings in UI
   ↓
2. SettingsController captures changes
   ↓
3. AppSettings object updated
   ↓
4. SettingsStore.save() called
   ↓
5. JSON serialization via Jackson
   ↓
6. Written to ~/.aitt/settings.json
   ↓
7. On application startup:
   - SettingsStore.load() reads JSON
   - AppSettings object reconstructed
   - UI re-populated with saved values
```

---

## Configuration & Persistence

### Storage Locations

**Application Directory: `~/.aitt/`**
```
~/.aitt/
├── settings.json              # User preferences (AppSettings)
├── sessions/                  # Persisted flashing sessions
│   ├── <uuid-1>.json
│   ├── <uuid-2>.json
│   └── ...
├── profiles/                  # Custom target profiles
│   ├── custom_chipset.json
│   └── ...
└── logs/
    └── aitt.log               # Application logs (rolling)
```

### Configuration Files

**Built-in Profiles: `resources/profiles/`**
```json
[
  {
    "id": "mpc5777m",
    "name": "MPC5777M Dual-Core",
    "cpuType": "MPC5777M",
    "flashStart": "0x00000000",
    "flashEnd": "0x01000000",
    "ramAddress": "0x40000000",
    "t32ConfigPath": "/path/to/t32_mpc5777m.cmm",
    "description": "Power ISA dual-core processor"
  },
  ...
]
```

**Theme Files: `resources/css/`**
```
style.css              # Default dark theme
style-light.css        # Light theme
carousel-demo.css      # Carousel styling
```

**FXML Layouts: `resources/fxml/`**
```
login.fxml             # Login screen
dashboard.fxml         # Main dashboard
flashing.fxml          # Flashing module UI
hardware.fxml          # Hardware discovery UI
remote.fxml            # Remote probe management UI
settings.fxml          # Settings dialog
config.fxml            # Configuration dialog
scripting.fxml         # Scripting module UI
```

---

## Native Layer Integration

### JNI Method Bindings

**Package: `com.aerospace.aitt.native_`**

```java
public class HardwareBridge {
    // Native declarations (implemented in C/C++)
    private native List<HardwareInfo> detectConnectedHardware0();
    private native boolean connectProbe0(String portName);
    private native void disconnectProbe0();
    private native CommandResult sendCommand0(String command);
    private native byte[] readMemory0(long address, int length);
    private native CommandResult writeMemory0(long address, byte[] data);
}
```

### C/C++ Side

**Native Implementation Structure:**
```
native/src/
├── aitt_native.c          # Core implementation
├── jni_bindings.c         # JNI method implementations
├── chipset_mpc5777m.c     # MPC5777M driver
└── chipset_s32k148.c      # S32K148 driver
```

**JNI Generated Headers:**
```
native/include/generated/
└── com_aerospace_aitt_native__HardwareBridge.h
    # Auto-generated from HardwareBridge.java by javah
```

### Build Process

**Maven Configuration:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-h</arg>
            <arg>${project.basedir}/native/include/generated</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

**CMake Build:**
```
native/
├── CMakeLists.txt          # Build configuration
└── build/
    └── aitt_native.sln     # Visual Studio project (Windows)
```

---

## Design Patterns

### 1. Singleton Pattern
**Used for:** Long-lived, application-scoped services
```
• AuthService - User authentication
• HardwareBridge - Native hardware access
• StageManager - Scene navigation
• ThemeManager - Theme management
• SettingsStore - Configuration persistence

Implementation:
• Static getInstance() method
• Double-checked locking for thread safety
• Volatile references
```

### 2. Immutable Record Pattern
**Used for:** Data models representing state
```
• FlashingSession - Flashing operation state
• SoftwareModule - Firmware binary representation
• TargetProfile - Chipset configuration
• NetworkProbe - Remote probe information
• User - User identity

Benefits:
• Thread-safe by design
• Value semantics
• Reduced bugs from accidental mutation
• JSON serializable
```

### 3. Service Orchestration Pattern
**Used by:** FlashingService
```
Coordinates multiple sub-services:
• Validator - Input validation
• CmmGenerator - Script generation
• Trace32Runner - Process execution
• SessionStore - Persistence

Workflow:
1. Validate inputs
2. Transform to intermediate representations
3. Execute steps
4. Persist results
5. Provide async callbacks
```

### 4. Observer/Listener Pattern
**Used by:** NetworkProbeService, FlashingService
```
• Listeners registered for events
• Asynchronous notification
• Decouples producers from consumers

Usage:
• Probe discovery notifications
• Progress callbacks
• Status updates
```

### 5. Template Method Pattern
**Used by:** BaseController
```
• Abstract base class defines lifecycle
• initialize() - After FXML loading
• onShow() - When view becomes visible
• onHide() - When view hidden
• onClose() - Cleanup and dispose

Subclasses override methods as needed
```

### 6. Factory Pattern
**Used by:** SoftwareModule.fromFile()
```
• Static factory method for complex object creation
• Handles file I/O and validation
• Returns fully-initialized module

Example:
SoftwareModule mod = SoftwareModule.fromFile(
    Paths.get("firmware.bin"),
    0x00001000
);
```

### 7. Strategy Pattern
**Used by:** File loading in CMM generation
```
Different strategies for different file types:
• .bin → Data.LOAD.Binary
• .hex → Data.LOAD.IntelHEX
• .elf → Data.LOAD.Elf

Encapsulated in getLoadCommand() method
```

### 8. Chain of Responsibility
**Used by:** Validation
```
Validator chains multiple validation checks:
1. File existence
2. Address validation
3. Size validation
4. Overlap detection
5. Duplicate address check

Each check responsible for specific validation
```

---

## Integration Points

### For CMM Generation Directory Integration

**1. Extend HardwareAbstractionLayer**
```
Create new implementations for:
• Different chipsets (S32G, MPC57xx, etc.)
• New debug protocols
• Custom memory layouts
```

**2. Custom CMM Generators**
```
Extend CmmGenerator for:
• Hardware-aware generation
• Advanced verification
• Chipset-specific protocols
• Performance optimizations
```

**3. Profile Library Extensions**
```
Add new target profiles:
• Create JSON in resources/profiles/
• Register with ProfileStore
• Implement HAL for chipset
```

**4. Test Definitions**
```
Create test scripts:
• CMM test templates
• Validation routines
• Memory verification
• State assertions
```

**5. Native Driver Integration**
```
Extend native layer for:
• New debug adapters
• Custom protocols
• Hardware-specific optimizations
• Performance improvements
```

### API Contracts

**FlashingService Contract:**
```
Input:
  • List<SoftwareModule> - Firmware files
  • TargetProfile - Target chipset
  • Path - CMM output location
  • Path - TRACE32 executable path

Output:
  • CompletableFuture<FlashingSession> - Result
  • Progress callbacks (0.0-1.0)
  • Log callbacks (strings)

Error Handling:
  • ValidationResult with error details
  • NativeException from native layer
  • IOException from file operations
```

**CmmGenerator Contract:**
```
Input:
  • FlashingSession - Complete session

Output:
  • String - CMM script content

Guarantees:
  • Valid TRACE32 CMM syntax
  • Session ID tracking
  • Timestamp included
  • Per-module progress reporting
```

---

## Summary Table

| Component | Type | Scope | Thread-Safe | Persistence |
|-----------|------|-------|------------|-------------|
| AuthService | Singleton Service | Application | Yes (volatile) | Session |
| FlashingService | Singleton Service | Application | Yes (volatile) | Via SessionStore |
| HardwareBridge | Singleton Service | Application | Yes (native mutex) | No |
| StageManager | Singleton Service | Application | Yes (volatile) | No |
| SettingsStore | Singleton Service | Application | Yes (volatile) | File (~/.aitt/settings.json) |
| CmmGenerator | Stateless | Module | Yes | Via external storage |
| Validator | Stateless | Module | Yes | No |
| Trace32Runner | Stateless | Module | Yes | No |
| FlashingSession | Record | Immutable | Yes | File (~/.aitt/sessions/) |
| SoftwareModule | Record | Immutable | Yes | Via FlashingSession |
| TargetProfile | Record | Immutable | Yes | File (resources/profiles/) |
| NetworkProbe | Record | Immutable | Yes | No (transient) |
| AppSettings | Data Class | Mutable | Yes (with synchronization) | File (~/.aitt/settings.json) |

---

## Project Statistics

- **Java Source Files:** ~30+
- **Java LOC:** ~3,500+
- **Native (C/C++) Files:** 4 (core + chipset drivers)
- **FXML Layouts:** 8
- **CSS Themes:** 2
- **Configuration Profiles:** 2+ (extensible)
- **Build System:** Maven + CMake
- **Java Version:** 20
- **JNI Interfaces:** 1 (HardwareBridge)
- **Singletons:** 6
- **Immutable Records:** 5

---

**END OF ARCHITECTURE DOCUMENTATION**

This document provides complete insight into the AITT codebase structure, ready for integration planning with your new CMM generation directory.

