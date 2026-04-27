/**
 * AITT Native Hardware Bridge
 * 
 * JNI implementation for low-level hardware communication
 * with debug probes and automotive chipsets.
 * 
 * Supported chipsets:
 *   - NXP S32K148 (ARM Cortex-M4F)
 *   - NXP MPC5777M (Power Architecture)
 * 
 * Build: cmake -B build -G "Visual Studio 17 2022" && cmake --build build --config Release
 */

#ifndef AITT_NATIVE_H
#define AITT_NATIVE_H

#include <jni.h>
#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// ============================================================================
// VERSION INFO
// ============================================================================

#define AITT_NATIVE_VERSION_MAJOR 1
#define AITT_NATIVE_VERSION_MINOR 0
#define AITT_NATIVE_VERSION_PATCH 0
#define AITT_NATIVE_VERSION_STRING "1.0.0"

// ============================================================================
// ERROR CODES
// ============================================================================

#define AITT_OK                     0
#define AITT_ERROR_NOT_INITIALIZED  -1
#define AITT_ERROR_ALREADY_INIT     -2
#define AITT_ERROR_INVALID_PARAM    -3
#define AITT_ERROR_NOT_FOUND        -4
#define AITT_ERROR_CONNECTION       -5
#define AITT_ERROR_TIMEOUT          -6
#define AITT_ERROR_IO               -7
#define AITT_ERROR_MEMORY           -8
#define AITT_ERROR_UNSUPPORTED      -9
#define AITT_ERROR_PERMISSION       -10
#define AITT_ERROR_BUSY             -11
#define AITT_ERROR_UNKNOWN          -99

// ============================================================================
// PROBE TYPES
// ============================================================================

typedef enum {
    PROBE_TYPE_UNKNOWN = 0,
    PROBE_TYPE_USB,
    PROBE_TYPE_SERIAL,
    PROBE_TYPE_JTAG,
    PROBE_TYPE_SWD
} ProbeType;

typedef enum {
    PROBE_STATUS_UNKNOWN = 0,
    PROBE_STATUS_AVAILABLE,
    PROBE_STATUS_IN_USE,
    PROBE_STATUS_ERROR
} ProbeStatus;

// ============================================================================
// STRUCTURES
// ============================================================================

/**
 * Hardware probe information
 */
typedef struct {
    ProbeType type;
    char id[64];
    char name[128];
    ProbeStatus status;
    char vendorId[16];
    char productId[16];
} ProbeInfo;

/**
 * Chipset configuration
 */
typedef struct {
    char chipsetId[32];
    char cpu[32];
    uint64_t flashStart;
    uint64_t flashEnd;
    uint64_t ramStart;
    uint64_t ramEnd;
    char debugInterface[16];
    uint32_t clockSpeed;
} ChipsetConfig;

/**
 * Command result
 */
typedef struct {
    int errorCode;
    char response[4096];
    char errorMessage[256];
} CommandResult;

// ============================================================================
// API FUNCTIONS
// ============================================================================

/**
 * Initialize the native subsystem.
 */
int aitt_init(void);

/**
 * Shutdown the native subsystem.
 */
void aitt_shutdown(void);

/**
 * Check if initialized.
 */
bool aitt_is_initialized(void);

/**
 * Get version string.
 */
const char* aitt_get_version(void);

/**
 * Get last error message.
 */
const char* aitt_get_last_error(void);

/**
 * Detect connected hardware probes.
 * @param probes Output array
 * @param maxProbes Maximum number of probes to detect
 * @return Number of probes found, or negative error code
 */
int aitt_detect_hardware(ProbeInfo* probes, int maxProbes);

/**
 * Detect available serial ports.
 * @param ports Output array of port names
 * @param maxPorts Maximum ports
 * @param portBufSize Size of each port name buffer
 * @return Number of ports found
 */
int aitt_detect_ports(char** ports, int maxPorts, int portBufSize);

/**
 * Connect to a probe.
 * @param probeId Probe identifier
 * @return AITT_OK or error code
 */
int aitt_connect(const char* probeId);

/**
 * Disconnect from current probe.
 */
int aitt_disconnect(void);

/**
 * Check if connected.
 */
bool aitt_is_connected(void);

/**
 * Send a CMM command.
 * @param command Command string
 * @param result Output result
 * @param timeoutMs Timeout in milliseconds
 * @return AITT_OK or error code
 */
int aitt_send_command(const char* command, CommandResult* result, int timeoutMs);

/**
 * Read memory from target.
 * @param address Start address
 * @param buffer Output buffer
 * @param length Number of bytes
 * @return Bytes read or negative error code
 */
int aitt_read_memory(uint64_t address, uint8_t* buffer, int length);

/**
 * Write memory to target.
 * @param address Start address
 * @param data Data to write
 * @param length Number of bytes
 * @return Bytes written or negative error code
 */
int aitt_write_memory(uint64_t address, const uint8_t* data, int length);

/**
 * Configure a specific chipset.
 * @param config Chipset configuration
 * @return AITT_OK or error code
 */
int aitt_configure_chipset(const ChipsetConfig* config);

// ============================================================================
// CHIPSET-SPECIFIC MODULES (for extension)
// ============================================================================

/**
 * Chipset driver interface - implement for each supported chipset
 */
typedef struct ChipsetDriver {
    const char* chipsetId;
    int (*init)(const ChipsetConfig* config);
    void (*shutdown)(void);
    int (*sendCommand)(const char* cmd, CommandResult* result);
    int (*readMemory)(uint64_t addr, uint8_t* buf, int len);
    int (*writeMemory)(uint64_t addr, const uint8_t* data, int len);
} ChipsetDriver;

/**
 * Register a chipset driver.
 */
int aitt_register_driver(const ChipsetDriver* driver);

/**
 * Get driver for chipset.
 */
const ChipsetDriver* aitt_get_driver(const char* chipsetId);

#ifdef __cplusplus
}
#endif

#endif // AITT_NATIVE_H
