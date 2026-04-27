# AITT Hardware & Remote Modules - Comprehensive Description

## Executive Summary

The AITT (Aerospace Integrated Test Toolkit) project implements two complementary debug probe management modules:

- **HARDWARE Module** - Manages locally-connected debug probes via USB/JTAG/Serial
- **REMOTE Module** - Manages network-attached debug probes via TCP/IP

Together, they provide comprehensive coverage of all debugging scenarios in automotive development.

---

## HARDWARE MODULE

### Purpose & Responsibilities

The Hardware module provides a **JavaFX-based UI for managing locally-connected debug probes**. It bridges the native C/JNI layer with the user interface, enabling:

- ✓ **Probe Detection** - Scan for connected USB/JTAG/serial debug probes
- ✓ **Port Scanning** - Enumerate available COM/serial ports
- ✓ **Connection Management** - Connect/disconnect from debug hardware
- ✓ **Command Execution** - Send CMM (TRACE32 macro language) commands
- ✓ **Real-time Logging** - Display hardware responses and status messages
- ✓ **Multi-Chipset Support** - ARM Cortex-M4F and Power ISA architectures

### Project Structure

```
src/main/java/com/aerospace/aitt/hardware/
└── ui/
    └── HardwareController.java        (JavaFX UI Controller)

src/main/resources/fxml/
└── hardware.fxml                      (UI Layout)

Dependencies:
├─ HardwareService                     (High-level wrapper)
├─ HardwareBridge (JNI)               (Native communication)
├─ aitt_native.dll                     (Native library)
└─ Core infrastructure
```

### Core Components

#### 1. HardwareController
**Role**: JavaFX UI controller managing the hardware probe interface

**Responsibilities**:
- Initialize UI components and bind event handlers
- Manage probe scanning and detection workflows
- Display real-time log output and status
- Handle user interactions (connect, disconnect, send commands)
- Integrate with HardwareService for async operations
- Manage loading states and progress indicators

**Key Methods**:
```java
@FXML void handleScanHardware()           // Scan for connected probes
@FXML void handleScanPorts()              // Scan for available serial ports
@FXML void handleConnect()                // Establish connection to selected probe
@FXML void handleDisconnect()             // Terminate probe connection
@FXML void handleSendCommand()            // Send custom CMM command
void sendQuickCommand(String cmd)         // Send preset commands
@Override public void initialize()        // Controller initialization
```

#### 2. HardwareService
**Role**: High-level async wrapper around HardwareBridge

**Responsibilities**:
- Provide non-blocking hardware operations via CompletableFuture
- Manage connection state and events
- Maintain probe lifecycle
- Log all hardware interactions
- Handle async exceptions and timeouts

**Key APIs**:
```java
// Detection
CompletableFuture<List<HardwareInfo>> detectHardwareAsync()
CompletableFuture<List<String>> detectPortsAsync()
List<HardwareInfo> detectHardware()
List<String> detectAvailablePorts()

// Connection Management
CompletableFuture<Boolean> connectAsync(String probeId, String chipset)
CompletableFuture<Boolean> disconnectAsync()

// Command Execution
CompletableFuture<String> sendCommandAsync(String command)

// Event Management
void addConnectionListener(Consumer<ConnectionEvent> listener)
void addLogListener(Consumer<String> listener)

// Info
String getNativeVersion()
```

#### 3. HardwareBridge (Native JNI Bridge)
**Role**: Java-to-C bridge for hardware communication

**Responsibilities**:
- Load native DLL dynamically from JAR or system path
- Marshal Java types to/from C types
- Execute native hardware operations (detect, read, write, execute)
- Handle initialization and cleanup
- Report errors through error codes

**Provides**:
- 12 native methods for hardware operations
- Thread-safe singleton pattern
- Automatic DLL extraction for portability
- Robust error handling

### Data Models

#### HardwareInfo Class

```java
public class HardwareInfo {
    public enum ProbeType {
        USB_JTAG,      // USB-connected JTAG probe
        USB_TRACE,     // USB-connected TRACE32 probe
        SERIAL,        // Serial port connection
        I2C,           // I2C debug interface
        CUSTOM,        // Custom interface
        UNKNOWN        // Unknown type
    }
    
    public enum ProbeStatus {
        AVAILABLE,     // Ready for connection
        BUSY,          // Currently in use
        ERROR,         // Error state
        DISCONNECTED   // Not connected
    }
    
    String id                          // Unique probe identifier
    String displayName()               // User-friendly name
    ProbeType type                     // Connection type
    ProbeStatus status                 // Current state
    String interfaceInfo              // Additional details
}
```

#### ConnectionEvent Record

```java
public record ConnectionEvent(
    ConnectionState state,    // DISCONNECTED, CONNECTING, CONNECTED, ERROR
    String probeId,          // Connected probe ID
    String message           // Status message
) {}
```

### UI Layout (hardware.fxml)

**Main Structure**: BorderPane with three regions

**Top Section** (Header):
```
┌──────────────────────────────────────┐
│ ← Back  Hardware Probe Manager        │
│ Target: [S32K148 ▼]  Status: Ready    │
│ ████████░  Loading...                 │
└──────────────────────────────────────┘
```

**Center Section** (40% Left | 60% Right Split):

**Left Panel - Probe Management**:
```
┌─────────────────────────────┐
│ Hardware Detection          │
│ [Scan Probes] [Scan Ports]  │
│                             │
│ Available Probes:           │
│ ┌─────────────────────────┐ │
│ │ ✓ USB JTAG Probe #1     │ │
│ │   Status: Available     │ │
│ │ ◐ COM Port /dev/ttyUSB0 │ │
│ │ ✗ Probe #2 (Error)      │ │
│ └─────────────────────────┘ │
│ [Connect] [Disconnect]      │
│                             │
│ Probe Info:                 │
│ ID: JTAG-001               │
│ Type: USB_JTAG             │
│ Status: Available          │
└─────────────────────────────┘
```

**Right Panel - Commands & Logging**:
```
┌──────────────────────────────┐
│ Commands                     │
│ ┌────────────────────────────┤
│ │ [System.Up] [Reset]        │
│ │ [Break]     [Go]           │
│ │ [Other...]                 │
│ └────────────────────────────┤
│                              │
│ Custom Command:              │
│ [________________] [Send]    │
│                              │
│ Response Log:                │
│ ┌────────────────────────────┤
│ │ > System.Up                │
│ │ OK                         │
│ │ > Break                    │
│ │ OK                         │
│ │ > Reset                    │
│ │ OK                         │
│ └────────────────────────────┤
│ [Clear Log]                  │
└──────────────────────────────┘
```

**Status Bar** (Footer):
```
Connected: Probe-001 | Type: USB_JTAG | Native: v1.0.0
```

### Targeted Chipsets

| Chipset | Architecture | CPU | Memory | Features |
|---------|----------|-----|--------|----------|
| **NXP S32K148** | ARMv7-M | Cortex-M4F | 1.5MB+256KB | Automotive Safety MCU |
| **NXP MPC5777M** | Power ISA | Dual e200z | 8MB+384KB | High-Performance |

### Integration Points

**With Native Module**:
- Depends on HardwareService + HardwareBridge
- Uses JNI for C hardware driver access
- Automatic DLL loading on startup

**With Core Infrastructure**:
- Extends `BaseController` from `core.ui`
- Uses `AppLogger` from `core.log`
- Uses `Alerts` notifications
- Uses `StageManager` for navigation

**With Dashboard**:
- Accessible via menu or button
- Returns to dashboard via "Back" button
- Part of main application workflow

---

## REMOTE MODULE

### Purpose & Responsibilities

The Remote module provides **network-based discovery and management of TRACE32 and AITT probe servers**. It handles:

- ✓ **Network Discovery** - UDP broadcast + subnet scanning
- ✓ **Manual Registration** - Add probes by IP/hostname
- ✓ **Connection Management** - Establish/maintain TCP connections
- ✓ **Remote Execution** - Send commands over network
- ✓ **Remote Flashing** - Execute firmware operations remotely
- ✓ **Keep-Alive** - Periodic heartbeat for connection stability

### Project Structure

```
src/main/java/com/aerospace/aitt/remote/
├── model/
│   └── NetworkProbe.java              (Data class & enums)
├── service/
│   ├── NetworkProbeService.java       (Discovery & management)
│   └── RemoteFlashingService.java     (Remote firmware flashing)
└── ui/
    └── RemoteProbeController.java     (JavaFX UI Controller)

src/main/resources/fxml/
└── remote.fxml                        (UI Layout)
```

### Core Components

#### 1. NetworkProbeService
**Role**: Orchestrates all network probe operations

**Responsibilities**:
- **Discovery**: UDP broadcast and subnet scanning
- **Connection**: Manage TCP connections to remote probes
- **Monitoring**: Keep-alive checks and status tracking
- **Events**: Notify listeners of discovery/connection events

**Discovery Methods**:
```java
// Parallel discovery (UDP + subnet scanning)
CompletableFuture<List<NetworkProbe>> discoverAll()

// UDP broadcast to port 20101
CompletableFuture<List<NetworkProbe>> discoverViaBroadcast()

// Scan subnet for TRACE32 and AITT probes
CompletableFuture<List<NetworkProbe>> scanLocalSubnet()

// Probe specific host
CompletableFuture<NetworkProbe> probeHost(String host, int port, RemoteType type)
```

**Connection APIs**:
```java
// Connection Management
CompletableFuture<Boolean> connect(NetworkProbe probe)
void disconnect(NetworkProbe probe)
Optional<Socket> getConnection(NetworkProbe probe)
boolean isConnected(NetworkProbe probe)

// Manual Registration
CompletableFuture<NetworkProbe> addManualProbe(String name, String host, int port, RemoteType type)

// Keep-Alive
void startKeepAlive()
void stopKeepAlive()

// Event Listeners
void addDiscoveryListener(Consumer<NetworkProbe> listener)
void removeDiscoveryListener(Consumer<NetworkProbe> listener)
void addLogListener(Consumer<String> listener)

// Info
List<NetworkProbe> getKnownProbes()
List<NetworkProbe> getConnectedProbes()
```

#### 2. RemoteFlashingService
**Role**: Orchestrates remote firmware flashing operations

**Responsibilities**:
- Validate firmware modules for remote target
- Establish remote flashing session
- Transfer firmware over network
- Execute flashing script remotely
- Handle aborts and error recovery
- Report progress and status

**Key Methods**:
```java
// Validation
ValidationResult validate(List<SoftwareModule> modules, TargetProfile target)

// Remote Flashing Operation
CompletableFuture<FlashingSession> executeRemote(
    NetworkProbe probe,
    List<SoftwareModule> modules,
    TargetProfile target,
    Consumer<String> logCallback,          // Progress logging
    Consumer<Double> progressCallback)     // Progress percentage

// Session Control
void abort()                               // Cancel operation
```

#### 3. RemoteProbeController
**Role**: JavaFX UI controller for network probe management

**Responsibilities**:
- Initialize remote probe UI
- Manage discovery workflow
- Handle probe connection/disconnection
- Display probe details and status
- Launch remote flashing operations
- Manage log display

**Key Methods**:
```java
@FXML void handleBack()                   // Return to dashboard
@FXML void handleDiscover()               // Start network discovery
@FXML void handleAddManual()              // Open manual add dialog
@FXML void handleConnect()                // Connect to selected probe
@FXML void handleDisconnect()             // Disconnect from probe
@FXML void handleTestConnection()         // Test probe availability
@FXML void handleRemove()                 // Remove probe from list
@FXML void handleFlashRemote()            // Launch remote flashing
@FXML void handleSendCommand()            // Send command to probe
```

### Data Models

#### NetworkProbe Record

```java
public record NetworkProbe(
    String id,                 // Unique ID: "net:ip:port"
    String name,              // Display name
    String host,              // IP address or hostname
    int port,                 // TCP port
    RemoteType type,          // TRACE32, AITT, etc.
    ConnectionStatus status,  // Current state
    Instant lastSeen,         // Last activity timestamp
    String firmware,          // Firmware version (optional)
    String serialNumber       // Serial number (optional)
) {
    public String address() { return host + ":" + port; }
    public String displayString() { ... }
    
    // Factory methods
    static NetworkProbe discovered(String host, int port, RemoteType type)
    static NetworkProbe manual(String name, String host, int port, RemoteType type)
}
```

#### RemoteType Enum

```java
public enum RemoteType {
    T32_POWERDEBUG("TRACE32 PowerDebug"),      // USB-attached PowerDebug
    T32_REMOTE_API("TRACE32 Remote API"),      // TRACE32 network API
    AITT_PROBE_SERVER("AITT Probe Server"),    // Native AITT server
    TCP_DEBUG_BRIDGE("TCP Debug Bridge"),      // Generic TCP bridge
    UNKNOWN("Unknown");                        // Unknown type
    
    String displayName()
}
```

#### ConnectionStatus Enum

```java
public enum ConnectionStatus {
    DISCOVERED("Discovered"),          // Found but not connected
    CONNECTING("Connecting..."),       // Connection in progress
    CONNECTED("Connected"),            // Ready for operations
    BUSY("Busy"),                      // Connected but processing
    DISCONNECTED("Disconnected"),      // Lost connection
    UNREACHABLE("Unreachable"),        // Not responding
    AUTH_REQUIRED("Auth Required"),    // Authentication needed
    ERROR("Error");                    // Error state
    
    boolean isAvailable()              // DISCOVERED or CONNECTED
    String displayName()
}
```

### UI Layout (remote.fxml)

**Main Structure**: BorderPane with header, left panel, and right panel

**Top Section** (Header):
```
┌───────────────────────────────────────┐
│ ← Back  Remote Probe Manager           │
│ Network-Attached Probes                │
│ Connection Status: Ready                │
│ ████████░  Discovering...              │
└───────────────────────────────────────┘
```

**Center Section** (350px Left | Expanding Right Split):

**Left Panel - Probe List**:
```
┌──────────────────────────────┐
│ Discovered Probes (3)        │
│ [Discover] [+ Add Manual]    │
│ ◐ Searching...               │
│                              │
│ ┌────────────────────────────┤
│ │ ✓ TRACE32-Lab02            │ (Green)
│ │   192.168.1.50:20000       │
│ │ ✓ AITT-Probe-01            │ (Green)
│ │   192.168.1.51:20100       │
│ │ ◐ PowerDebug-Unit          │ (Orange)
│ │   192.168.1.52:20000       │
│ └────────────────────────────┤
│ [- Remove]                   │
└──────────────────────────────┘
```

**Right Panel - Connection & Actions**:
```
┌─────────────────────────────────────┐
│ Connection Control                  │
│ [Connect] [Disconnect] [Test]       │
│                                     │
│ Probe Information:                  │
│ ┌─────────────────────────────────┐ │
│ │ Name:      TRACE32-Lab02        │ │
│ │ Type:      TRACE32 Remote API   │ │
│ │ Address:   192.168.1.50:20000   │ │
│ │ Status:    Connected            │ │
│ │ Last Seen: 2 minutes ago         │ │
│ │ Firmware:  v3.2.1               │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Remote Operations:                  │
│ [Flash Remote] [Send Command]       │
│                                     │
│ Command Response:                   │
│ ┌─────────────────────────────────┐ │
│ │ > System.Up                     │ │
│ │ OK                              │ │
│ │ [________________] [Send]       │ │
│ └─────────────────────────────────┐ │
│ [Clear Log]                         │
└─────────────────────────────────────┘
```

### Network Protocols

#### 1. UDP Broadcast Discovery

**Configuration**:
- **Port**: 20101 (discovery port)
- **Timeout**: 3000ms
- **Broadcast Address**: 255.255.255.255

**Discovery Packet**:
```
Request:  "AITT_PROBE_DISCOVER" (UTF-8)
Response: "AITT_PROBE:port:name:type:firmware:serial"
```

**Example Response**:
```
AITT_PROBE:20100:Probe-001:AITT_PROBE_SERVER:v1.2.3:SN-ABC123
```

#### 2. TCP Subnet Scanning

**Scan Range**:
- Automatic subnet detection from local IP
- Scans all hosts in /24 subnet (254 hosts)
- Parallel connections for speed

**Ports Checked**:
- Port 20000 (TRACE32 PowerDebug default)
- Port 20100 (AITT Probe Server default)

**Service Identification**:
```
For AITT Probes:
  → Send: "AITT_PING\n"
  → Expect: "AITT_PONG..."
  → Confirm: AITT_PROBE_SERVER type

For TRACE32:
  → Port openness = availability
  → Service type inferred from port
```

**Timing**:
- Connection timeout: 2000ms per probe
- Total discovery: 6000ms (3s broadcast + 3s scan)
- Parallel workers: 4 threads

#### 3. Remote Flashing Protocol (Text-based)

**Session Establishment**:
```
FLASH_START:target:modulecount
Response: OK:sessionid  or  ERROR:code:message
```

**Module Transfer** (base64 encoded):
```
FLASH_MODULE:name:address:size:base64data
Response: OK:checksum  or  ERROR:code:message
Chunk size: 64KB (max per packet)
Timeout: 60 seconds
```

**Script Transfer**:
```
FLASH_SCRIPT:base64script
Response: OK:scriptid  or  ERROR:code:message
```

**Execution**:
```
FLASH_EXECUTE
Response: PROGRESS:percentage  (repeated)
         OK:result  (on success)
         ERROR:code:message  (on failure)
Timeout: 300 seconds (5 minutes)
```

**Status Query**:
```
FLASH_STATUS
Response: {status_json}
```

**Abort**:
```
FLASH_ABORT
Response: OK or ERROR
```

### Integration Points

**With NetworkProbeService**:
- Establishes connections to network probes
- Gets connection sockets for data transfer
- Receives discovery events from listener
- Checks connection status before flashing

**With Flashing Module**:
- Uses `Validator` for module validation
- Uses `CmmGenerator` for TRACE32 script generation
- Uses `FlashingSession` for operation tracking
- Imports from `flashing.model` and `flashing.service`

**With Core Infrastructure**:
- Extends `BaseController` from `core.ui`
- Uses `AppLogger` from `core.log`
- Uses `Alerts` for dialogs and notifications
- Uses `StageManager` for scene management

**Threading Model**:
- Discovery: 4-thread pool + scheduled executor
- Keep-alive: Scheduled executor (30-second interval)
- Flashing: Separate executor for long-running operations

---

## ARCHITECTURE & DESIGN PATTERNS

### Layered Architecture

```
┌──────────────────────────────────────┐
│    Controllers (JavaFX UI Layer)     │
│  HardwareController RemoteProbeCtrl  │
└─────────┬────────────────────────────┘
          │ Depends on
┌─────────▼────────────────────────────┐
│   Services (Business Logic Layer)    │
│  HardwareService NetworkProbeService │
│  RemoteFlashingService               │
└─────────┬────────────────────────────┘
          │ Depends on
┌─────────▼────────────────────────────┐
│   Models (Data Objects & Enums)      │
│  HardwareInfo NetworkProbe           │
│  FlashingSession ConnectionStatus    │
└─────────┬────────────────────────────┘
          │ Uses
┌─────────▼────────────────────────────┐
│  Native Layer (JNI / Sockets)        │
│  HardwareBridge aitt_native.dll      │
└──────────────────────────────────────┘
```

### Design Patterns Implemented

#### 1. **Async/Await Pattern**
- All blocking operations wrapped in `CompletableFuture`
- Non-blocking UI updates via `Platform.runLater()`
- Exception handling via `.exceptionally()` chaining

```java
handleDetectProbes() {
    service.detectHardwareAsync()
        .thenAccept(probes -> Platform.runLater(() -> updateUI(probes)))
        .exceptionally(e -> handleError(e));
}
```

#### 2. **Observer Pattern**
Event listeners for state changes:
- `addConnectionListener()` for connection events
- `addDiscoveryListener()` for probe discoveries
- `addLogListener()` for operation logging

#### 3. **Service Locator Pattern**
Lazy-loaded singleton services:
```java
HardwareBridge bridge = HardwareBridge.getInstance();  // Created once
NetworkProbeService service = new NetworkProbeService(); // Created once
```

#### 4. **Executor Pattern**
Dedicated thread pools:
- Hardware: Single daemon thread
- Network: 4-thread pool for parallel discovery
- Scheduler: Single thread for keep-alive

#### 5. **Builder/Factory Pattern**
Immutable record creation:
```java
NetworkProbe probe = NetworkProbe.discovered(host, port, type);
NetworkProbe manual = NetworkProbe.manual(name, host, port, type);
```

#### 6. **Template Method Pattern**
BaseController provides lifecycle template
```java
class HardwareController extends BaseController {
    @Override initialize() { /* Setup */ }
    @Override onEnter() { /* On visibility */ }
}
```

---

## USAGE WORKFLOWS

### Hardware Module Workflow

```
User navigates to Hardware Manager
    ↓
[Scan Probes] button clicked
    ↓
HardwareController.handleScanHardware()
    ↓
HardwareService.detectHardwareAsync()
    ↓
HardwareBridge.nativeDetectHardware() → Native DLL
    ↓
Returns: List<HardwareInfo>
    ↓
UI ListView updated with available probes
    ↓
User selects probe + clicks [Connect]
    ↓
HardwareService.connectAsync()
    ↓
HardwareBridge.nativeConnectProbe()
    ↓
Connection established
    ↓
User sends commands via TextField
    ↓
HardwareService.sendCommandAsync()
    ↓
Response logged in TextArea
```

### Remote Module Workflow

```
User navigates to Remote Manager
    ↓
[Discover] button clicked
    ↓
RemoteProbeController.handleDiscover()
    ↓
NetworkProbeService.discoverAll()
    ├─→ discoverViaBroadcast()       (Parallel)
    └─→ scanLocalSubnet()             (Parallel)
        ├─→ probeHost(192.168.x.1)    (100+ parallel tasks)
        ├─→ probeHost(192.168.x.2)
        └─→ ...
    ↓
Discovery listeners fire with found probes
    ↓
UI ListView populated with discovered probes
    ↓
Color-coded status display:
    🟢 DISCOVERED (blue) - findable but not connected
    🔵 CONNECTED (green) - actively connected
    🟠 CONNECTING (orange) - connection in progress
    🔴 ERROR (red) - connection failed
    ↓
User selects probe + clicks [Connect]
    ↓
NetworkProbeService.connect(probe)
    ↓
TCP connection established to host:port
    ↓
Keep-alive heartbeat started (30-second interval)
    ↓
User clicks [Flash Remote]
    ↓
RemoteFlashingService.executeRemote()
    ├─→ Validate firmware modules
    ├─→ Establish flashing session
    ├─→ Transfer modules (base64 encoded, 64KB chunks)
    ├─→ Transfer CMM script
    └─→ Execute flashing
    ↓
Progress callback updates progress bar
    ↓
Completion or error result returned
```

---

## KEY FEATURES COMPARISON

### Hardware Module

| Feature | Capability |
|---------|-----------|
| **Discovery Method** | USB/JTAG/Serial enumeration |
| **Probes** | Local only |
| **Latency** | Ultra-low (JNI direct) |
| **Throughput** | Limited by USB/serial bandwidth |
| **Scalability** | 1 probe at a time |
| **Performance** | Native C performance |
| **Complexity** | C DLL management |

### Remote Module

| Feature | Capability |
|---------|-----------|
| **Discovery Method** | UDP broadcast + subnet scan |
| **Probes** | Network-accessible |
| **Latency** | Network-dependent (100ms+) |
| **Throughput** | 100Mbps+ Ethernet |
| **Scalability** | Multiple probes supported |
| **Performance** | Text-based protocol |
| **Complexity** | Async network handling |

---

## TECHNICAL SPECIFICATIONS

### Threading Model

| Component | Thread Type | Count | Purpose |
|-----------|------------|-------|---------|
| HardwareService | Daemon | 1 | Hardware async operations |
| NetworkProbeService Discovery | Daemon | 4 | Parallel network scanning |
| NetworkProbeService Keep-Alive | Daemon | 1 | Connection monitoring |
| JavaFX Platform | UI Thread | 1 | All UI updates |

### Network Configuration

| Parameter | Value | Purpose |
|-----------|-------|---------|
| Discovery Port | 20101 | UDP broadcast |
| TRACE32 Port | 20000 | PowerDebug default |
| AITT Port | 20100 | AITT Probe default |
| Discovery Timeout | 3000ms | UDP wait time |
| Connection Timeout | 2000ms | TCP handshake |
| Keep-Alive Interval | 30000ms | Heartbeat frequency |

### Performance Characteristics

| Operation | Typical Time |
|-----------|------------|
| Local probe detection | 100-500ms |
| Network discovery | 6000ms (3s bcast + 3s scan) |
| Connect local | 50-100ms |
| Connect remote | 200-500ms |
| Memory read (1MB) | 50-200ms |
| Memory write (1MB) | 100-300ms |
| Flash small firmware | 5-30 seconds |
| Flash large firmware | 30-300 seconds |

---

## SUMMARY

Together, the **Hardware** and **Remote** modules provide:

✅ **Complete Coverage** - Local and network debug probes  
✅ **User-Friendly UI** - Intuitive probe discovery and management  
✅ **Async Operations** - Non-blocking UI with progress feedback  
✅ **Robust Networking** - UDP discovery + TCP connections  
✅ **Multi-Protocol Support** - TRACE32 + AITT + generic TCP  
✅ **Error Handling** - Comprehensive error codes and recovery  
✅ **Real-Time Feedback** - Logging and status display  
✅ **Extensibility** - Pluggable architecture for new probe types  

These modules are critical components of the AITT application, enabling seamless debugging of automotive embedded systems whether probes are connected locally or accessible over the network.
