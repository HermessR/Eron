# AITT Native Library

JNI native library for low-level hardware communication with debug probes and automotive chipsets.

## Supported Chipsets

- **NXP S32K148** - ARM Cortex-M4F automotive MCU (SWD debug)
- **NXP MPC5777M** - Power Architecture dual-core MCU (JTAG debug)

## Project Structure

```
native/
├── CMakeLists.txt      # CMake build configuration
├── build.bat           # Windows build script
├── README.md           # This file
├── include/
│   └── aitt_native.h   # Public API header
├── src/
│   ├── aitt_native.c       # Core implementation
│   ├── jni_bindings.c      # JNI method implementations
│   ├── chipset_s32k148.c   # S32K148 driver
│   └── chipset_mpc5777m.c  # MPC5777M driver
└── build/              # Build output (generated)
```

## Prerequisites

- **Windows 10/11** (64-bit)
- **Visual Studio 2019/2022** with C++ desktop development
- **CMake 3.16+**
- **JDK 17+** (JAVA_HOME must be set)

## Building

### Using build script (recommended)

```batch
cd native
build.bat
```

### Using CMake directly

```batch
cd native
cmake -B build -G "Visual Studio 17 2022" -A x64
cmake --build build --config Release
```

### Using Ninja (faster builds)

```batch
cd native
cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

## Output

After building, the DLL is automatically copied to:
```
src/main/resources/native/windows-x64/aitt_native.dll
```

## Adding New Chipsets

1. Create `src/chipset_<name>.c` following the driver template
2. Implement the `ChipsetDriver` interface:
   - `init()` - Initialize chipset configuration
   - `shutdown()` - Clean up resources
   - `sendCommand()` - Handle CMM commands
   - `readMemory()` - Read target memory
   - `writeMemory()` - Write target memory
3. Add registration function call in `aitt_native.c:aitt_init()`
4. Add source file to `CMakeLists.txt`

### Driver Template

```c
#include "aitt_native.h"

static int myChip_init(const ChipsetConfig* config) { ... }
static void myChip_shutdown(void) { ... }
static int myChip_send_command(const char* cmd, CommandResult* result) { ... }
static int myChip_read_memory(uint64_t addr, uint8_t* buf, int len) { ... }
static int myChip_write_memory(uint64_t addr, const uint8_t* data, int len) { ... }

static const ChipsetDriver myChip_driver = {
    .chipsetId = "MY_CHIP",
    .init = myChip_init,
    .shutdown = myChip_shutdown,
    .sendCommand = myChip_send_command,
    .readMemory = myChip_read_memory,
    .writeMemory = myChip_write_memory
};

int myChip_register_driver(void) {
    return aitt_register_driver(&myChip_driver);
}
```

## Java Integration

The native library is loaded automatically by `HardwareBridge.java`:

```java
// Get instance (loads native library automatically)
HardwareBridge bridge = HardwareBridge.getInstance();

// Detect hardware
List<HardwareInfo> devices = bridge.detectConnectedHardware();

// Configure chipset
bridge.configureChipset("S32K148", ChipsetConfig.forS32K148());

// Connect to probe
bridge.connectProbe("USB:0001");

// Send command
CommandResult result = bridge.sendCommand("System.Up");

// Read memory
byte[] data = bridge.readMemory(0x08000000, 256);
```

## API Reference

### Core Functions

| Function | Description |
|----------|-------------|
| `aitt_init()` | Initialize native subsystem |
| `aitt_shutdown()` | Shutdown and cleanup |
| `aitt_detect_hardware()` | Enumerate debug probes |
| `aitt_detect_ports()` | List serial ports |
| `aitt_connect()` | Connect to probe |
| `aitt_disconnect()` | Disconnect from probe |
| `aitt_send_command()` | Execute CMM command |
| `aitt_read_memory()` | Read target memory |
| `aitt_write_memory()` | Write target memory |
| `aitt_configure_chipset()` | Set chipset configuration |

### Error Codes

| Code | Name | Description |
|------|------|-------------|
| 0 | AITT_OK | Success |
| -1 | NOT_INITIALIZED | Subsystem not initialized |
| -3 | INVALID_PARAM | Invalid parameter |
| -4 | NOT_FOUND | Device not found |
| -5 | CONNECTION | Connection error |
| -6 | TIMEOUT | Operation timed out |
| -9 | UNSUPPORTED | Unsupported operation |

## License

Proprietary - Aerospace Integration Testing Tool
