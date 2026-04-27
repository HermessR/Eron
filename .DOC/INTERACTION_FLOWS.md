# AITT Module Interaction & Communication Contracts

## Overview

This document details the communication contracts and interaction flows between the Frontend (UI), Backend (Application Services), Flashing Module, Remote Module, and Hardware Module.

---

## 1. Firmware Flashing Use Case Flow

### Complete Interaction Sequence

```
┌─────────────┐         ┌──────────────┐         ┌─────────┐         ┌────────────┐         ┌──────────┐
│  Frontend   │         │   Backend    │         │Flashing │         │  Hardware  │         │  TRACE32 │
│ Flashing    │         │  Services    │         │  Module │         │   Module   │         │ External │
│ Controller  │         │              │         │         │         │            │         │          │
└────┬────────┘         └──────┬───────┘         └────┬────┘         └────┬───────┘         └────┬─────┘
     │                         │                      │                    │                      │
     │  1. selectModules()     │                      │                    │                      │
     ├────────────────────────>│                      │                    │                      │
     │                         │                      │                    │                      │
     │  2. validate()          │                      │                    │                      │
     ├────────────────────────>│                      │                    │                      │
     │    ValidationRequest    │                      │                    │                      │
     │  {modules, target}      │                      │                    │                      │
     │                         │  3. validate()      │                    │                      │
     │                         ├─────────────────────>│                    │                      │
     │                         │                      │ Check memory       │                      │
     │                         │                      │ overlaps           │                      │
     │                         │  4. ValidationResult │                    │                      │
     │                         │<─────────────────────┤                    │                      │
     │  5. ValidationResult    │                      │                    │                      │
     │<────────────────────────┤                      │                    │                      │
     │  {valid, errors,        │                      │                    │                      │
     │   warnings}             │                      │                    │                      │
     │                         │                      │                    │                      │
     │ [User clicks Execute]   │                      │                    │                      │
     │                         │                      │                    │                      │
     │  6. execute()           │                      │                    │                      │
     ├────────────────────────>│                      │                    │                      │
     │    FlashingRequest      │                      │                    │                      │
     │  {modules, target,      │                      │                    │                      │
     │   callbacks}            │                      │                    │                      │
     │                         │                      │                    │                      │
     │  7. [async begins]      │                      │                    │                      │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                      │                    │                      │
     │  CompletableFuture      │                      │                    │                      │
     │                         │                      │                    │                      │
     │                         │  8. validator.      │                    │                      │
     │                         │     validate()      │                    │                      │
     │                         ├─────────────────────>│                    │                      │
     │                         │                      │                    │                      │
     │                         │  9. cmmGenerator.   │                    │                      │
     │                         │     generate()      │                    │                      │
     │                         ├─────────────────────>│                    │                      │
     │                         │                      │ Parse modules      │                      │
     │                         │                      │ Generate CMM       │                      │
     │                         │<─ CMM script ──────>│                    │                      │
     │                         │                      │                    │                      │
     │                         │ 10. trace32Runner.  │                    │                      │
     │                         │     execute()       │                    │                      │
     │                         ├─────────────────────>│                    │                      │
     │                         │                      │                    │ Invoke TRACE32       │
     │                         │                      │                    ├───────────────────> │
     │                         │                      │                    │ (subprocess call)   │
     │                         │                      │                    │                      │
     │ 11. progressCallback    │                      │                    │                      │
     │<─ ─ (0.0) ─ ─ ─ ─ ─ ─ ─<                      │                    │                      │
     │ [UI shows progress]     │                      │                    │                      │
     │                         │                      │                    │                      │
     │ 12. progressCallback    │                      │                    │                      │
     │<─ ─ (0.25) ─ ─ ─ ─ ─ ─ <                      │                    │                      │
     │                         │                      │                    │ TRACE32 flashing    │
     │ 13. progressCallback    │                      │                    │ in progress...       │
     │<─ ─ (0.5) ─ ─ ─ ─ ─ ─ ─<                      │                    │                      │
     │                         │                      │                    │                      │
     │ 14. logCallback         │                      │                    │                      │
     │<─ ─ "Flashing..." ─ ─ ─<                      │                    │                      │
     │                         │                      │                    │ [Execution complete]│
     │                         │                      │                    │<────────────────────┤
     │ 15. progressCallback    │                      │                    │                      │
     │<─ ─ (1.0) ─ ─ ─ ─ ─ ─ ─<                      │                    │                      │
     │                         │                      │                    │                      │
     │                         │ 16. sessionStore.   │                    │                      │
     │                         │     save()          │                    │                      │
     │                         ├─────────────────────>│                    │                      │
     │                         │                      │ [Archive session]  │                      │
     │                         │<─────────────────────┤                    │                      │
     │ 17. FlashingSession     │                      │                    │                      │
     │     (Completed) ─ ─ ─ ─ <                      │                    │                      │
     │     Result received     │                      │                    │                      │
     │                         │                      │                    │                      │
     │ [Show results]          │                      │                    │                      │
     │ [Enable UI controls]    │                      │                    │                      │
     │                         │                      │                    │                      │
```

### Message Contracts

#### 2. Validation Request
```json
{
  "type": "ValidationRequest",
  "modules": [
    {
      "name": "bootloader",
      "version": "1.0.0",
      "binaryPath": "/path/to/bootloader.bin",
      "baseAddress": "0x00000000",
      "size": 65536,
      "checksum": "sha256:abc123..."
    }
  ],
  "targetProfile": {
    "id": "s32k148",
    "name": "NXP S32K148",
    "cpuType": "ARM Cortex-M4F",
    "flashStart": "0x00000000",
    "flashEnd": "0x00200000",
    "ramAddress": "0x1fff0000"
  }
}
```

#### 4. Validation Response
```json
{
  "valid": true,
  "errors": [],
  "warnings": [
    "Module 'app' uses 95% of available flash"
  ]
}
```

#### 6. Flashing Request
```json
{
  "modules": [
    { "name": "bootloader", "binaryPath": "/path", "baseAddress": "0x00000000", "size": 65536 },
    { "name": "app", "binaryPath": "/path", "baseAddress": "0x00010000", "size": 1048576 }
  ],
  "targetProfile": { "id": "s32k148", ... },
  "outputPath": "/output",
  "runTrace32": true,
  "trace32Path": "C:\\T32\\bin\\t32marm.exe",
  "callbacks": {
    "logCallback": "Function<String>",
    "progressCallback": "Function<Double>"
  }
}
```

#### 17. Flashing Session (Final Result)
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "modules": [...],
  "targetProfile": {...},
  "status": "COMPLETED",
  "generatedScript": "/output/flash_1234567890.cmm",
  "startTime": "2024-03-30T14:23:45",
  "endTime": "2024-03-30T14:25:12",
  "result": {
    "success": true,
    "bytesFlashed": 1114112,
    "duration": 87000,
    "message": "Flashing completed successfully"
  }
}
```

---

## 2. Hardware Probe Discovery & Connection

### Complete Interaction Sequence

```
┌─────────────┐         ┌──────────────┐       ┌────────────┐       ┌──────────────┐       ┌─────────────┐
│  Frontend   │         │   Backend    │       │ Hardware   │       │    Native    │       │   Physical  │
│ Hardware    │         │  Hardware    │       │   Bridge   │       │   Library    │       │  USB/JTAG   │
│ Controller  │         │  Service     │       │   (JNI)    │       │ (C/C++)      │       │   Probe     │
└────┬────────┘         └──────┬───────┘       └────┬───────┘       └──────┬───────┘       └────┬────────┘
     │                         │                     │                      │                     │
     │ 1. onModuleInit()       │                     │                      │                     │
     │ Auto-discover probes    │                     │                      │                     │
     │                         │                     │                      │                     │
     │ 2. discoverProbes()     │                     │                      │                     │
     ├────────────────────────>│                     │                      │                     │
     │ [async call]            │                     │                      │                     │
     │                         │ 3. JNI call        │                      │                     │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                     │                      │                     │
     │ CompletableFuture       │ discoverProbes()   │                      │                     │
     │                         ├────────────────────>│                      │                     │
     │                         │                     │ 4. Native discovery │                     │
     │                         │                     ├─────────────────────>│                     │
     │                         │                     │                      │ Enumerate USB      │
     │                         │                     │                      │ COM ports           │
     │                         │                     │                      ├────────────────────>│
     │                         │                     │                      │ (Windows API)       │
     │                         │                     │                      │                     │
     │                         │                     │                      │ Query each port     │
     │                         │                     │                      │<────────────────────┤
     │                         │                     │                      │                     │
     │                         │                     │ 5. Probe detection   │                     │
     │                         │                     │<─────────────────────┤                     │
     │                         │ ProbeInfo[]         │ (VID, PID, serial)   │                     │
     │                         │<────────────────────┤                      │                     │
     │ 6. fireDiscoveryEvent() │                     │                      │                     │
     │<─────(List<HardwareInfo>)─────────────────────┤                      │                     │
     │                         │                     │                      │                     │
     │ [Update UI probe list]  │                     │                      │                     │
     │ Display: "USB Port A"   │                     │                      │                     │
     │          "COM3"         │                     │                      │                     │
     │                         │                     │                      │                     │
     │ [User selects probe]    │                     │                      │                     │
     │ [User selects chipset S32K148]                │                      │                     │
     │                         │                     │                      │                     │
     │ 7. connect()            │                     │                      │                     │
     ├────────────────────────>│                     │                      │                     │
     │ {probeId: "USB001",     │                     │                      │                     │
     │  chipsetConfig: {...}}  │                     │                      │                     │
     │                         │ 8. JNI connect()   │                      │                     │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                     │                      │                     │
     │ CompletableFuture       ├────────────────────>│                      │                     │
     │                         │                     │ 9. Native init      │                     │
     │                         │                     ├─────────────────────>│                     │
     │                         │                     │ s32k148_init(config) │                     │
     │                         │                     │                      │ Initialize SWD     │
     │                         │                     │                      ├────────────────────>│
     │                         │                     │                      │ Set freq, pins      │
     │                         │                     │                      │<────────────────────┤
     │                         │                     │                      │ Device responds     │
     │                         │                     │<─────────────────────┤                     │
     │                         │ 10. Success         │                      │                     │
     │                         │<────────────────────┤                      │                     │
     │ 11. ConnectionEvent     │                     │                      │                     │
     │     {CONNECTED,         │                     │                      │                     │
     │      "USB001", "OK"}    │                     │                      │                     │
     │<─────(via callback)─────┤                     │                      │                     │
     │                         │                     │                      │                     │
     │ [Enable command buttons]│                     │                      │                     │
     │ [Show "Connected"]      │                     │                      │                     │
     │                         │                     │                      │                     │
```

### Connection Message Contracts

#### 2. Probe Discovery Request
```cpp
// C struct representation
typedef struct {
    int timeout_ms;  // 30000ms default
} ProbeDiscoveryRequest_t;
```

#### 6. Discovery Result (List of HardwareInfo)
```json
[
  {
    "probeId": "USB001",
    "probeType": "USB",
    "serialNumber": "ABC12345",
    "firmwareVersion": "2.1.0",
    "supportedChipsets": ["S32K148", "MPC5777M"]
  },
  {
    "probeId": "COM3",
    "probeType": "SERIAL",
    "serialNumber": "PC123456",
    "firmwareVersion": "1.8.2",
    "supportedChipsets": ["S32K148"]
  }
]
```

#### 7. Connection Request
```json
{
  "probeId": "USB001",
  "chipsetConfig": {
    "chipsetId": "S32K148",
    "debugPort": "USB001",
    "debugFrequency": 8,
    "resetType": "System"
  }
}
```

#### 11. Connection Event (via callbacks)
```json
{
  "state": "CONNECTED",
  "probeId": "USB001",
  "message": "Successfully connected to USB001 with S32K148"
}
```

---

## 3. Remote Probe Discovery & Network Connection

### Complete Interaction Sequence

```
┌─────────────┐         ┌──────────────┐       ┌────────────────┐       ┌──────────────────┐
│  Frontend   │         │   Backend    │       │  Network Probe │       │    LAN / WAN     │
│  Remote     │         │    Network   │       │   Discovery    │       │                  │
│ Controller  │         │ Probe Service│       │                │       │  Broadcast       │
└────┬────────┘         └──────┬───────┘       └────┬────────────┘       │  Subnet Scan     │
     │                         │                     │                    │  mDNS            │
     │ 1. onModuleInit()       │                     │                    │                  │
     │                         │                     │                    │                  │
     │ 2. discover()           │                     │                    │                  │
     ├────────────────────────>│                     │                    │                  │
     │                         │ 3. UDP Broadcast   │                    │                  │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                     │                    │                  │
     │ CompletableFuture       │ to 255.255.255.255 │                    │                  │
     │                         ├────────────────────────────────────────>│                  │
     │                         │ :20101             │                    │ Broadcast ack    │
     │                         │ DISCOVERY_REQ      │                    │                  │
     │                         │                     │<────────────────────────────────────┤
     │                         │ 4. Unicast replies │                    │                  │
     │                         │<─────────────────────────────────────────────────────────┤
     │                         │ DISCOVERY_RESP     │                    │                  │
     │                         │ (probeId, IP, etc) │                    │                  │
     │                         │                     │                    │                  │
     │                         │ 5. Subnet scan     │                    │                  │
     │                         ├────────────────────────────────────────>│                  │
     │                         │ TCP connect to     │                    │                  │
     │                         │ 192.168.1.100-200  │                    │ Try port 20101   │
     │                         │ :20101             │                    │                  │
     │                         │                     │<────────────────────────────────────┤
     │                         │ 6. Successful TCP  │                    │                  │
     │                         │ connections        │                    │                  │
     │                         │<─────────────────────────────────────────────────────────┤
     │                         │                     │                    │                  │
     │                         │ 7. mDNS query      │                    │                  │
     │                         │ *.local            │                    │ mDNS response    │
     │                         ├────────────────────────────────────────>│                  │
     │                         │                     │                    │                  │
     │                         │ 8. Aggregated      │                    │                  │
     │                         │ results            │                    │                  │
     │  9. fireDiscoveryEvent()│<────────────────────────────────────────┤                  │
     │<─────(List<NetworkProbe>)──────────────────────┤                    │                  │
     │                         │                     │                    │                  │
     │ [Update UI probe list]  │                     │                    │                  │
     │ Display: "probe-001"    │                     │                    │                  │
     │          "192.168.1.50" │                     │                    │                  │
     │          "AITT_SERVER"  │                     │                    │                  │
     │                         │                     │                    │                  │
     │ [User selects probe]    │                     │                    │                  │
     │                         │                     │                    │                  │
     │ 10. connect()           │                     │                    │                  │
     ├────────────────────────>│                     │                    │                  │
     │ {probeId, IP, port,     │                     │                    │                  │
     │  timeout}               │ 11. TCP connect    │                    │                  │
     │                         ├────────────────────────────────────────>│                  │
     │                         │ 192.168.1.50:20101 │                    │ Accept connection│
     │                         │                     │<────────────────────────────────────┤
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                     │                    │                  │
     │ CompletableFuture       │ 12. Handshake      │                    │ Send ID packet   │
     │                         │<─────(ID exchange)─────────────────────────────────────┤
     │                         │                     │                    │                  │
     │                         │ 13. Start keepalive│                    │                  │
     │                         │ timer (30sec)      │                    │                  │
     │ 14. RemoteConnectionEvent                    │                    │                  │
     │     {CONNECTED,         │                    │                    │                  │
     │      "probe-001"}       │                    │                    │                  │
     │<─────(via callback)─────┤                    │                    │                  │
     │                         │                    │                    │                  │
     │ [Show "Connected"]      │                    │                    │                  │
     │ [Enable remote ops]     │                    │                    │                  │
     │                         │                    │                    │                  │
```

### Network Message Contracts

#### 3. UDP Broadcast Discovery Beacon
```
Destination: 255.255.255.255:20101
Message Format (binary):
┌────────────────────────────────────────┐
│ uint16: version (1)                    │
│ uint16: type (DISCOVERY_REQ = 1)       │
│ uint16: optional_filter (probeType)    │
└────────────────────────────────────────┘
```

#### 4. UDP Discovery Response
```
Source: <remote_ip>:20101
Message Format (binary/ASCII):
┌────────────────────────────────────────┐
│ uint16: version                        │
│ uint16: type (DISCOVERY_RESP = 2)      │
│ String: probeId ("aitt-probe-001")     │
│ String: ipAddress ("192.168.1.50")     │
│ uint16: port (20101)                   │
│ uint16: probeType (1=AITT_SERVER)      │
│ String: supportedChipsets              │
└────────────────────────────────────────┘
```

#### 10. Remote Connection Request
```json
{
  "probeId": "aitt-probe-001",
  "ipAddress": "192.168.1.50",
  "port": 20101,
  "timeout": 5000
}
```

---

## 4. Hardware Command Execution

### Complete Interaction Sequence

```
┌─────────────┐         ┌──────────────┐       ┌────────────┐       ┌──────────────┐       ┌──────────────┐
│  Frontend   │         │   Backend    │       │ Hardware   │       │    Native    │       │   Physical   │
│ Hardware    │         │  Hardware    │       │   Bridge   │       │   Library    │       │ Debug Target │
│ Controller  │         │  Service     │       │   (JNI)    │       │ (Chipset)    │       │              │
└────┬────────┘         └──────┬───────┘       └────┬───────┘       └──────┬───────┘       └────┬─────────┘
     │                         │                     │                      │                     │
     │ 1. commandInput()       │                     │                      │                     │
     │ User enters CMM command │                     │                      │                     │
     │                         │                     │                      │                     │
     │ 2. sendCommand()        │                     │                      │                     │
     ├────────────────────────>│                     │                      │                     │
     │ "R.S SWIO 1"            │                     │                      │                     │
     │                         │ 3. JNI call        │                      │                     │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                     │                      │                     │
     │ CompletableFuture       │ sendCommand()      │                      │                     │
     │                         ├────────────────────>│                      │                     │
     │                         │                     │ 4. Route to driver   │                     │
     │                         │                     ├─────────────────────>│                     │
     │                         │                     │                      │ s32k148_send_cmd() │
     │                         │                     │                      ├────────────────────>│
     │                         │                     │                      │ SWIO protocol      │
     │                         │                     │                      │ transmission       │
     │                         │                     │                      │                     │
     │                         │                     │                      │ [Bitbanging]       │
     │                         │                     │                      ├───────────────────>│
     │                         │                     │                      │ SWD pins pulse     │
     │                         │                     │                      │<───────────────────┤
     │                         │                     │                      │ Target responds    │
     │                         │                     │                      │                     │
     │                         │                     │ 5. Response          │                     │
     │                         │                     │<─────────────────────┤                     │
     │                         │ CommandResult       │ (success, data,      │                     │
     │                         │<────────────────────┤  errorCode)          │                     │
     │ 6. CommandResult        │                     │                      │                     │
     │ {success: true,         │                     │                      │                     │
     │  message: "OK",         │                     │                      │                     │
     │  data: [...]}           │                     │                      │                     │
     │<─────────────────────────────────────────────┤                      │                     │
     │                         │                     │                      │                     │
     │ 7. fireLogEvent()       │                     │                      │                     │
     │ "CMD: R.S SWIO 1"       │                     │                      │                     │
     │ "RESP: OK"              │                     │                      │                     │
     │                         │                     │                      │                     │
     │ [Display in log pane]   │                     │                      │                     │
     │                         │                     │                      │                     │
```

### Command Message Contracts

#### 2. Command Execution Request
```json
{
  "command": "R.S SWIO 1",
  "timeout": 5000
}
```

#### 6. Command Execution Response (CommandResult)
```json
{
  "success": true,
  "message": "Register set successfully",
  "data": "abc123...(hex bytes)",
  "errorCode": 0
}
```

#### 7. Log Event (async callback)
```json
{
  "timestamp": "2024-03-30T14:23:45.123",
  "level": "INFO",
  "message": "Command 'R.S SWIO 1' executed: OK"
}
```

---

## 5. Remote Flashing Operation

### Complete Interaction Sequence

```
┌─────────────┐         ┌──────────────────┐       ┌──────────────────────┐       ┌──────────────┐
│  Frontend   │         │   Backend Remote │       │   Remote Probe      │       │    Network   │
│  Remote     │         │   Flashing       │       │  (TCP server)        │       │              │
│ Controller  │         │  Service         │       │                      │       │  TCP Socket  │
└────┬────────┘         └──────┬───────────┘       └────┬──────────────────┘       └────┬─────────┘
     │                         │                        │                            │
     │ 1. selectModules()      │                        │                            │
     │ selectFirmware()        │                        │                            │
     │                         │                        │                            │
     │ 2. executeRemoteFlashing│                        │                            │
     ├────────────────────────>│                        │                            │
     │ {remoteProbeId,         │                        │                            │
     │  modules[],             │                        │                            │
     │  targetProfile,         │                        │                            │
     │  progressCallback}      │                        │                            │
     │                         │                        │                            │
     │<─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─<                        │                            │
     │ CompletableFuture       │                        │                            │
     │                         │ 3. Serialize modules  │                            │
     │                         │ (binary blob)         │                            │
     │                         │                        │                            │
     │                         │ 4. Create TCP Frame   │                            │
     │                         │ {magic, type, size,   │                            │
     │                         │  checksum, payload}   │                            │
     │                         │                        │                            │
     │                         │ 5. Send over TCP      │                            │
     │                         ├───────────────────────────────────────────────────>│
     │                         │ [Socket write]        │                            │
     │                         │                        │ 6. Receive ACK            │
     │                         │<───────────────────────────────────────────────────┤
     │                         │ (Frame acknowledged)  │                            │
     │                         │                        │                            │
     │                         │                        │ 7. Process request        │
     │                         │                        ├──────────────────────────>│
     │                         │                        │ Parse modules             │
     │                         │                        │ Generate CMM              │
     │                         │                        │ Invoke TRACE32            │
     │                         │                        │ [Flashing in progress]    │
     │                         │                        │                            │
     │                         │                        │ 8. Progress Update        │
     │                         │ 9. Progress Update    │ (stage=VALIDATING,        │
     │                         │<───────────────────────│  percentage=0.1)          │
     │                         │ {sessionId, %,        │                            │
     │                         │  stage, message}      │                            │
     │ 10. progressCallback()  │                        │                            │
     │<─ ─ ─(0.1, VALIDATING)──<                        │                            │
     │ [UI progress bar: 10%]  │                        │                            │
     │                         │                        │                            │
     │                         │                        │ [Flashing continues...]   │
     │                         │ 11. Progress Update   │                            │
     │                         │<───────────────────────│ percentage=0.5            │
     │ 12. progressCallback()  │                        │                            │
     │<─ ─ ─(0.5, FLASHING)────<                        │                            │
     │ [UI progress bar: 50%]  │                        │                            │
     │                         │                        │                            │
     │                         │                        │ [Flashing complete]       │
     │                         │ 13. Completion        │                            │
     │                         │ Result frame          │                            │
     │                         │<───────────────────────│ {success: true,            │
     │                         │ {sessionId, success,   │  bytesFlashed: 1114112,   │
     │                         │  message, duration}    │  duration: 87000}         │
     │                         │                        │                            │
     │ 14. Completion callback │                        │                            │
     │<─ ─ ─(result)─ ─ ─ ─ ─ ─<                        │                            │
     │ RemoteFlashingResult    │                        │                            │
     │                         │                        │                            │
     │ [Show status]           │                        │                            │
     │ "Flashing: SUCCESS"     │                        │                            │
     │ "Bytes: 1.1 MB"         │                        │                            │
     │ "Time: 1:27"            │                        │                            │
     │                         │                        │                            │
```

### Remote Flashing Message Contracts

#### 2. Remote Flashing Request
```json
{
  "remoteProbeId": "aitt-probe-001",
  "modules": [
    {
      "name": "bootloader",
      "size": 65536,
      "baseAddress": "0x00000000",
      "data": "[binary blob - gzip compressed]"
    }
  ],
  "targetProfile": {
    "id": "s32k148",
    "cpuType": "ARM Cortex-M4F",
    "flashStart": "0x00000000",
    "flashEnd": "0x00200000"
  }
}
```

#### 5. TCP Frame Format
```
┌──────────────────────────────────────────────┐
│ HEADER:                                      │
│  magic: uint32 = 0xDEADBEEF                  │
│  messageType: uint16 = FLASH_REQ (1)         │
│  payloadSize: uint32                         │
│  checksum: uint32 (CRC32)                    │
├──────────────────────────────────────────────┤
│ PAYLOAD:                                     │
│ [JSON-serialized RemoteFlashingRequest]      │
│ + binary firmware data                       │
└──────────────────────────────────────────────┘
```

#### 9. Progress Update (Stream)
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "percentage": 0.1,
  "stage": "VALIDATING",
  "message": "Validating modules against target profile",
  "bytesProcessed": 0,
  "estimatedRemaining": 80
}
```

#### 13. Completion Result
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "success": true,
  "remoteSessionId": "remote-sess-12345",
  "message": "Flashing completed successfully",
  "bytesFlashed": 1114112,
  "duration": 87000
}
```

---

## Communication Protocol Summary

| Direction | Module | Protocol | Format | Example |
|-----------|--------|----------|--------|---------|
| Frontend → Backend | Flashing | Method Call | Sync/Async (CompletableFuture) | `validate(...)` |
| Frontend → Backend | Hardware | Method Call | Async (CompletableFuture) | `discoverProbes()` |
| Frontend → Backend | Remote | Method Call | Async (CompletableFuture) | `discover()` |
| Backend → JNI | Hardware | JNI Bridge | Native calls | `Java_...sendCommand()` |
| JNI → Native | Chipset Driver | C API | Function pointers | `chipset_send_command()` |
| Backend → Remote | Network | TCP/IP | Custom binary frames | `MessageType.FLASH_REQ` |
| Remote → Backend | Network | TCP/IP | Custom binary frames | `MessageType.FLASH_RESP` |
| Backend → Frontend | Backend | Callbacks | Lambda functions | `Consumer<Double>` |
| Services → Services | Inter-service | Direct calls | Sync or Async | `validator.validate()` |

---

## Error Handling Contracts

### Exception Propagation

```
Native C Layer (error codes)
  ↓
JNI Layer (throws NativeException)
  ↓
HardwareBridge (propagates exception)
  ↓
HardwareService (catches, logs, fires error events)
  ↓
Frontend Controller (displays error dialog)
```

### Network Error Handling

```
TCP Connection Failure
  → Retry with exponential backoff
  → After 3 retries, fire ConnectionStateChanged event (ERROR)
  → Frontend shows error and prompts user to reconnect

UDP Discovery Timeout
  → Continue scanning other methods
  → Aggregate partial results after 3000ms
  → Return empty list if no probes found
```

---

## Summary of Key Communication Patterns

1. **Async Operations**: All I/O operations (hardware, network) return `CompletableFuture` for non-blocking execution
2. **Event-Driven**: Components use callbacks for progress, logging, and state changes
3. **Type-Safe Contracts**: Strongly-typed request/response objects ensure correctness
4. **Error Codes**: Native layer uses error codes; JNI converts to exceptions; backend converts to events
5. **Timeout Protection**: All network and hardware operations have configurable timeouts
6. **Serialization**: Complex objects serialized to JSON for network transmission and persistence

