# AITT - Automation Integration Test Tool

A professional-grade JavaFX desktop application for integration testing and firmware flashing of aeronautical embedded systems. AITT provides comprehensive hardware debugging, firmware validation, and network probe management capabilities for automotive and aerospace chipsets.

**Version:** 1.0.0  
**Platform:** Windows 10/11 (x64)  
**License:** Proprietary

---

## 📋 Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [System Requirements](#system-requirements)
- [Building](#building)
- [Running the Application](#running-the-application)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Key Components](#key-components)
- [Configuration](#configuration)
- [Supported Hardware](#supported-hardware)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

---

## ✨ Features

### Core Functionality

- **Firmware Flashing** - Load and verify firmware binaries on embedded systems with progress tracking
- **Hardware Management** - Automatic detection and management of USB/Serial debug probes
- **Remote Debugging** - Network-attached debug probe support via TCP and mDNS discovery
- **CMM Script Generation** - Automated TRACE32 CMM script generation for embedded systems
- **Multi-Chipset Support** - Pre-configured support for:
  - NXP MPC5777M (Power Architecture, JTAG debug)
  - NXP S32K148 (ARM Cortex-M4F, SWD debug)
- **Session Management** - Persist and recover flashing sessions with automatic state recovery
- **User Authentication** - Session-based user management with secure login
- **Real-time Monitoring** - Live hardware status, connection monitoring, and probe state tracking
- **Script Development** - Built-in scripting module for test automation
- **Configuration Management** - Persistent user preferences and target profiles

### Advanced Features

- **Hardware Abstraction Layer** - Unified interface across multiple chipsets via JNI
- **Async Command Processing** - Non-blocking operations with thread pooling
- **Memory Operations** - Direct memory read/write capabilities with validation
- **Checksum Verification** - Automatic firmware integrity verification
- **Custom Themes** - Light/dark theme support with CSS-based customization
- **Error Recovery** - Automatic reconnection and session recovery mechanisms

---

## 🚀 Quick Start

### Prerequisites
- **Java 20** or later
- **Maven 3.8+**
- Windows 10/11 (64-bit)

### Clone & Build
```bash
cd aitt
mvn clean compile
```

### Run the Application
```bash
# Launch with JavaFX
mvn javafx:run

# Or build and run JAR
mvn clean package
java -jar target/aitt-1.0.0.jar
```

---

## 💻 System Requirements

### Hardware
- **CPU:** Intel Core i5 / AMD Ryzen 5 (or equivalent)
- **RAM:** 4 GB minimum (8 GB recommended)
- **Storage:** 500 MB free space
- **USB Ports:** For debug probe connection

### Software
- **OS:** Windows 10 or Windows 11 (64-bit)
- **Java:** JDK 20 or higher
- **Maven:** 3.8.1 or higher
- **Visual C++ Runtime:** For native library support
- **TRACE32:** (Optional) For CMM script execution

### Development
- Visual Studio 2019/2022 (for native library compilation)
- CMake 3.20+ (for native library builds)

---

## 🔨 Building

### Building Java Application

```bash
# Full build with compilation and testing
mvn clean install

# Quick compile only
mvn clean compile

# Build JAR package
mvn clean package
```

### Building Native Library (Windows)

The native library (`aitt_native.dll`) provides low-level hardware communication:

```bash
cd aitt/native
./build.bat        # Automatic CMake + Visual Studio build
```

Or manually:
```bash
cd aitt/native/build
cmake --build . --config Release
```

**Note:** Native library is pre-built and included. Rebuild only if modifying C/C++ code.

---

## ▶️ Running the Application

### From IDE
```bash
# Using Maven
mvn javafx:run

# Or run Main.java directly
java com.aerospace.aitt.shell.MainApp
```

### From Command Line
```bash
# Build first
mvn clean package

# Run JAR
java -jar target/aitt-1.0.0.jar
```

### Application Flow
1. **Login Screen** - Authenticate with user credentials
2. **Dashboard** - View connected probes and system status
3. **Select Feature:**
   - **Flashing** - Upload and flash firmware
   - **Hardware** - Configure and monitor debug probes
   - **Remote** - Connect to network probes
   - **Scripting** - Develop and execute test scripts
4. **Results** - View logs and execution reports

---

## 🏗️ Architecture

AITT uses a **layered architecture** with clear separation of concerns:

### Architecture Layers

```
┌──────────────────────────────────┐
│   UI Layer (JavaFX Controllers)  │
├──────────────────────────────────┤
│   Service Layer (Business Logic) │
├──────────────────────────────────┤
│   Domain Models & Entities       │
├──────────────────────────────────┤
│   Data & Configuration Layer     │
├──────────────────────────────────┤
│   Native Layer (C/C++ via JNI)   │
└──────────────────────────────────┘
```

### Core Components

| Component | Purpose |
|-----------|---------|
| **FlashingService** | Orchestrates firmware flashing workflows |
| **HardwareService** | Manages probe detection and state |
| **NetworkProbeService** | Handles remote probe connections |
| **CmmGenerator** | Generates TRACE32 CMM scripts |
| **HardwareBridge** | JNI bridge to native layer |
| **SessionStore** | Persists flashing sessions |
| **ProfileStore** | Manages target device profiles |

---

## 📁 Project Structure

```
aitt/
├── pom.xml                          # Maven build configuration
├── native/                          # C/C++ native library
│   ├── CMakeLists.txt
│   ├── src/
│   │   ├── aitt_native.c           # Core native impl
│   │   ├── jni_bindings.c          # JNI wrappers
│   │   ├── chipset_s32k148.c       # S32K148 driver
│   │   └── chipset_mpc5777m.c      # MPC5777M driver
│   └── include/
│       └── aitt_native.h            # Native API header
│
├── src/main/java/com/aerospace/aitt/
│   ├── shell/                       # Application shell & startup
│   │   ├── MainApp.java
│   │   ├── LoginController.java
│   │   ├── DashboardController.java
│   │   └── ModuleLoader.java
│   │
│   ├── flashing/                    # Firmware flashing module
│   │   ├── ui/                      # UI Controllers
│   │   ├── service/                 # FlashingService, Validator
│   │   ├── data/                    # SessionStore, ProfileStore
│   │   └── model/                   # Domain models
│   │
│   ├── hardware/                    # Hardware management
│   │   ├── ui/
│   │   ├── service/                 # HardwareService
│   │   └── model/
│   │
│   ├── remote/                      # Remote probe module
│   │   ├── ui/
│   │   ├── service/                 # NetworkProbeService
│   │   └── model/
│   │
│   ├── backend/                     # Backend utilities
│   │   └── hardware/                # HAL, CMM generation
│   │
│   ├── scripting/                   # Test scripting module
│   │   └── ui/
│   │
│   ├── config/                      # Configuration management
│   │   ├── AppSettings.java
│   │   └── SettingsStore.java
│   │
│   ├── core/                        # Core utilities
│   │   ├── AppLogger.java
│   │   └── Alerts.java
│   │
│   └── native_/                     # Native integration
│       └── HardwareBridge.java      # JNI wrapper class
│
├── src/main/resources/
│   ├── fxml/                        # JavaFX UI definitions
│   ├── css/                         # Stylesheets
│   ├── sounds/                      # Audio resources
│   ├── native/                      # Native DLL files
│   └── profiles/                    # Default profiles
│
├── src/test/java/                   # Unit tests
└── target/                          # Build output

```

---

## 🔑 Key Components

### Flashing Module (`flashing/`)
Orchestrates firmware flashing in three phases:
1. **Validation** (10%) - Verify files, addresses, and boundaries
2. **Generation** (30%) - Generate CMM scripts
3. **Execution** (60%) - Execute via TRACE32

**Key Classes:**
- `FlashingService` - Main orchestrator
- `Validator` - Input validation
- `CmmGenerator` - TRACE32 script generation
- `Trace32Runner` - TRACE32 execution wrapper

### Hardware Module (`hardware/`)
Manages debug probes and chipset communication.

**Key Classes:**
- `HardwareService` - Async probe management
- `HardwareInfo` - Probe metadata
- `HardwareBridge` - JNI interface to native code

### Remote Module (`remote/`)
Network-based probe management with TCP/mDNS support.

**Key Classes:**
- `NetworkProbeService` - Remote probe lifecycle
- `NetworkProbe` - Remote probe model
- `RemoteProbeController` - UI controller

### Backend Utilities (`backend/`)
Low-level utilities for hardware interaction.

**Key Classes:**
- `HardwareAbstractionLayer` - Unified chipset interface
- `CMMScriptGenerator` - Script generation
- `MPCHardwareImpl` - Chipset-specific impl

---

## ⚙️ Configuration

### Application Settings
Located in `AppSettings.java`:
- User preferences (theme, language)
- TRACE32 executable path
- Thread pool configuration
- Logging level

### Persistent Stores
- `SessionStore` - `/data/sessions/*.json`
- `ProfileStore` - `/data/profiles/*.json`
- `SettingsStore` - `/data/settings.json`

### Logging
Configured via `logback.xml`:
- Console output
- File logging (rotating daily)
- Debug and info levels available

---

## 🎯 Supported Hardware

### Chipsets

#### NXP S32K148
- **Architecture:** ARM Cortex-M4F
- **Debug Interface:** SWD (Serial Wire Debug)
- **Use Case:** Automotive microcontroller
- **Driver:** `chipset_s32k148.c`

#### NXP MPC5777M
- **Architecture:** Power ISA Dual-Core
- **Debug Interface:** JTAG
- **Use Case:** Automotive Power Management
- **Driver:** `chipset_mpc5777m.c`

### Debug Probes
- **Local:** USB/Serial debug adapters (SEGGER, STLink, etc.)
- **Remote:** Network-attached probes via TCP
- **Discovery:** Automatic USB enumeration, mDNS for network probes

---

## 🛠️ Development

### Prerequisites
- Java 20 JDK
- Maven 3.8+
- Visual Studio 2019/2022 (for native code)
- CMake 3.20+

### Setting Up Development Environment

```bash
# Clone repository
git clone <repo-url>
cd Eron

# Install dependencies
cd aitt
mvn clean install

# Build native library (optional, if modifying native code)
cd native
./build.bat
cd ..

# Launch IDE
# Open in IntelliJ IDEA or Eclipse with Maven support
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FlashingServiceTest

# Skip tests during build
mvn package -DskipTests
```

### Building Native Library

To modify native C/C++ code:

```bash
cd aitt/native
# Edit source files in src/

# Rebuild
./build.bat

# Rebuild output: build/bin/aitt_native.dll
# Copy to: src/main/resources/native/aitt_native.dll
```

### Code Structure Guidelines

- **Controllers:** Handle UI events, call services
- **Services:** Implement business logic, coordinate operations
- **Models:** Domain entities, immutable or thread-safe
- **Utilities:** Pure functions, stateless helpers
- **Data:** Persistence layer, JSON serialization

---

## 🐛 Troubleshooting

### Build Issues

**Problem:** "Maven compilation fails with Java version mismatch"
```
Solution: Ensure Java 20+ is in PATH
java -version  # Should show 20+
```

**Problem:** "JavaFX runtime components are missing"
```
Solution: Maven should auto-download. Try:
mvn clean install -U
```

### Native Library Issues

**Problem:** "aitt_native.dll not found"
```
Solution: 
1. Check: src/main/resources/native/aitt_native.dll exists
2. Rebuild: cd native && build.bat
3. Run: mvn package
```

**Problem:** "Cannot load native library"
```
Solution:
1. Ensure Windows Visual C++ Runtime is installed
2. Check DLL compatibility (x64 only)
3. Review logs for detailed error
```

### Runtime Issues

**Problem:** "No USB debug probes detected"
```
Solution:
1. Check device manager for probe visibility
2. Install probe drivers (SEGGER, STLink, etc.)
3. Review hardware service logs
```

**Problem:** "CMM script execution fails"
```
Solution:
1. Verify TRACE32 path in settings
2. Check TRACE32 license validity
3. Review generated CMM script for errors
```

### Performance Issues

**Problem:** "Application freezes during long operations"
```
Solution:
1. Monitor is running on EDT by default
2. Long tasks use ExecutorService (FlashingService)
3. Check logs for blocking operations
```

---

## 📝 Logging

Logs are written to:
- **Console:** Real-time debug output
- **File:** `logs/aitt.log` (rotates daily)

To change log level, edit `src/main/resources/logback.xml`:
```xml
<root level="DEBUG">  <!-- INFO, DEBUG, TRACE -->
  ...
</root>
```

---

## 📚 Additional Resources

### Documentation Files
- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture
- [AITT_FULL_ARCHITECTURE.md](AITT_FULL_ARCHITECTURE.md) - Complete technical specs
- [APP_ARCHITECTURE_DIAGRAMS.md](APP_ARCHITECTURE_DIAGRAMS.md) - Visual diagrams
- [HARDWARE_MODULE_INTERACTION.md](INTERACTION_FLOWS.md) - Component interactions
- [native/README.md](aitt/native/README.md) - Native library docs

### Key Files
- Main Entry: [aitt/src/main/java/com/aerospace/aitt/shell/MainApp.java](aitt/src/main/java/com/aerospace/aitt/shell/MainApp.java)
- Build Config: [aitt/pom.xml](aitt/pom.xml)
- Native API: [aitt/native/include/aitt_native.h](aitt/native/include/aitt_native.h)

---

## 📞 Support

For issues, questions, or contributions:
1. Check existing documentation in the `*.md` files
2. Review log files in `logs/` directory
3. Inspect architecture diagrams for understanding module interactions
4. Contact development team

---

## 📄 License

This project is proprietary software. Unauthorized copying or distribution is prohibited.

**Copyright © 2026 Aerospace Technologies. All rights reserved.**

---

**Last Updated:** April 27, 2026  
**Maintained By:** Development Team
