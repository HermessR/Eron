/**
 * AITT Native Hardware Bridge - Main Implementation
 * 
 * JNI bindings and core functionality for hardware communication.
 */

#include "aitt_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include <setupapi.h>
#include <devguid.h>

// Link with SetupAPI for device enumeration
#pragma comment(lib, "setupapi.lib")

// ============================================================================
// INTERNAL STATE
// ============================================================================

static bool g_initialized = false;
static bool g_connected = false;
static char g_lastError[512] = "";
static char g_currentProbeId[64] = "";
static ChipsetConfig g_currentConfig = {0};
static CRITICAL_SECTION g_lock;

// Driver registry
#define MAX_DRIVERS 16
static const ChipsetDriver* g_drivers[MAX_DRIVERS] = {NULL};
static int g_driverCount = 0;
static const ChipsetDriver* g_activeDriver = NULL;

// ============================================================================
// INTERNAL HELPERS
// ============================================================================

static void set_error(const char* msg) {
    strncpy(g_lastError, msg, sizeof(g_lastError) - 1);
    g_lastError[sizeof(g_lastError) - 1] = '\0';
}

static void clear_error(void) {
    g_lastError[0] = '\0';
}

// ============================================================================
// CORE API IMPLEMENTATION
// ============================================================================

int aitt_init(void) {
    if (g_initialized) {
        set_error("Already initialized");
        return AITT_ERROR_ALREADY_INIT;
    }
    
    InitializeCriticalSection(&g_lock);
    clear_error();
    g_initialized = true;
    
    // Register built-in chipset drivers
    extern int s32k148_register_driver(void);
    extern int mpc5777m_register_driver(void);
    
    s32k148_register_driver();
    mpc5777m_register_driver();
    
    return AITT_OK;
}

void aitt_shutdown(void) {
    if (!g_initialized) return;
    
    EnterCriticalSection(&g_lock);
    
    if (g_connected) {
        aitt_disconnect();
    }
    
    // Shutdown all drivers
    for (int i = 0; i < g_driverCount; i++) {
        if (g_drivers[i] && g_drivers[i]->shutdown) {
            g_drivers[i]->shutdown();
        }
    }
    
    g_driverCount = 0;
    g_activeDriver = NULL;
    g_initialized = false;
    
    LeaveCriticalSection(&g_lock);
    DeleteCriticalSection(&g_lock);
}

bool aitt_is_initialized(void) {
    return g_initialized;
}

const char* aitt_get_version(void) {
    return AITT_NATIVE_VERSION_STRING;
}

const char* aitt_get_last_error(void) {
    return g_lastError;
}

// ============================================================================
// HARDWARE DETECTION
// ============================================================================

int aitt_detect_hardware(ProbeInfo* probes, int maxProbes) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!probes || maxProbes <= 0) {
        set_error("Invalid parameters");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    int count = 0;
    
    // Enumerate USB devices
    HDEVINFO deviceInfoSet = SetupDiGetClassDevs(
        &GUID_DEVCLASS_USB,
        NULL, NULL,
        DIGCF_PRESENT
    );
    
    if (deviceInfoSet != INVALID_HANDLE_VALUE) {
        SP_DEVINFO_DATA deviceInfoData;
        deviceInfoData.cbSize = sizeof(SP_DEVINFO_DATA);
        
        for (DWORD i = 0; 
             count < maxProbes && SetupDiEnumDeviceInfo(deviceInfoSet, i, &deviceInfoData); 
             i++) {
            
            char buffer[256];
            if (SetupDiGetDeviceRegistryPropertyA(
                    deviceInfoSet, &deviceInfoData,
                    SPDRP_DEVICEDESC, NULL,
                    (PBYTE)buffer, sizeof(buffer), NULL)) {
                
                // Filter for debug probes (J-Link, ST-Link, etc.)
                if (strstr(buffer, "J-Link") || 
                    strstr(buffer, "ST-Link") || 
                    strstr(buffer, "CMSIS-DAP") ||
                    strstr(buffer, "Debug") ||
                    strstr(buffer, "Trace32") ||
                    strstr(buffer, "Lauterbach")) {
                    
                    probes[count].type = PROBE_TYPE_USB;
                    snprintf(probes[count].id, sizeof(probes[count].id), "USB:%04d", i);
                    strncpy(probes[count].name, buffer, sizeof(probes[count].name) - 1);
                    probes[count].status = PROBE_STATUS_AVAILABLE;
                    count++;
                }
            }
        }
        
        SetupDiDestroyDeviceInfoList(deviceInfoSet);
    }
    
    // Enumerate COM ports for serial probes
    for (int port = 1; port <= 256 && count < maxProbes; port++) {
        char portName[32];
        snprintf(portName, sizeof(portName), "\\\\.\\COM%d", port);
        
        HANDLE hPort = CreateFileA(
            portName, GENERIC_READ | GENERIC_WRITE,
            0, NULL, OPEN_EXISTING, 0, NULL
        );
        
        if (hPort != INVALID_HANDLE_VALUE) {
            CloseHandle(hPort);
            
            probes[count].type = PROBE_TYPE_SERIAL;
            snprintf(probes[count].id, sizeof(probes[count].id), "COM%d", port);
            snprintf(probes[count].name, sizeof(probes[count].name), "Serial Port COM%d", port);
            probes[count].status = PROBE_STATUS_AVAILABLE;
            count++;
        }
    }
    
    LeaveCriticalSection(&g_lock);
    return count;
}

int aitt_detect_ports(char** ports, int maxPorts, int portBufSize) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    int count = 0;
    
    for (int port = 1; port <= 256 && count < maxPorts; port++) {
        char portName[32];
        snprintf(portName, sizeof(portName), "\\\\.\\COM%d", port);
        
        HANDLE hPort = CreateFileA(
            portName, GENERIC_READ | GENERIC_WRITE,
            0, NULL, OPEN_EXISTING, 0, NULL
        );
        
        if (hPort != INVALID_HANDLE_VALUE) {
            CloseHandle(hPort);
            snprintf(ports[count], portBufSize, "COM%d", port);
            count++;
        }
    }
    
    return count;
}

// ============================================================================
// CONNECTION MANAGEMENT
// ============================================================================

int aitt_connect(const char* probeId) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!probeId || strlen(probeId) == 0) {
        set_error("Invalid probe ID");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    
    if (g_connected) {
        aitt_disconnect();
    }
    
    // Store probe ID
    strncpy(g_currentProbeId, probeId, sizeof(g_currentProbeId) - 1);
    
    // Initialize active driver if configured
    if (g_activeDriver && g_activeDriver->init) {
        int result = g_activeDriver->init(&g_currentConfig);
        if (result != AITT_OK) {
            LeaveCriticalSection(&g_lock);
            return result;
        }
    }
    
    g_connected = true;
    clear_error();
    
    LeaveCriticalSection(&g_lock);
    return AITT_OK;
}

int aitt_disconnect(void) {
    if (!g_initialized) {
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    EnterCriticalSection(&g_lock);
    
    if (g_activeDriver && g_activeDriver->shutdown) {
        g_activeDriver->shutdown();
    }
    
    g_connected = false;
    g_currentProbeId[0] = '\0';
    clear_error();
    
    LeaveCriticalSection(&g_lock);
    return AITT_OK;
}

bool aitt_is_connected(void) {
    return g_connected;
}

// ============================================================================
// COMMAND EXECUTION
// ============================================================================

int aitt_send_command(const char* command, CommandResult* result, int timeoutMs) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!g_connected) {
        set_error("Not connected");
        return AITT_ERROR_CONNECTION;
    }
    
    if (!command || !result) {
        set_error("Invalid parameters");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    
    memset(result, 0, sizeof(CommandResult));
    
    // Use chipset-specific driver if available
    if (g_activeDriver && g_activeDriver->sendCommand) {
        int ret = g_activeDriver->sendCommand(command, result);
        LeaveCriticalSection(&g_lock);
        return ret;
    }
    
    // Default: echo command (placeholder for actual implementation)
    snprintf(result->response, sizeof(result->response), "OK: %s", command);
    result->errorCode = AITT_OK;
    
    LeaveCriticalSection(&g_lock);
    return AITT_OK;
}

// ============================================================================
// MEMORY ACCESS
// ============================================================================

int aitt_read_memory(uint64_t address, uint8_t* buffer, int length) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!g_connected) {
        set_error("Not connected");
        return AITT_ERROR_CONNECTION;
    }
    
    if (!buffer || length <= 0) {
        set_error("Invalid parameters");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    
    // Use chipset-specific driver if available
    if (g_activeDriver && g_activeDriver->readMemory) {
        int ret = g_activeDriver->readMemory(address, buffer, length);
        LeaveCriticalSection(&g_lock);
        return ret;
    }
    
    // Default: return zeros (placeholder)
    memset(buffer, 0, length);
    
    LeaveCriticalSection(&g_lock);
    return length;
}

int aitt_write_memory(uint64_t address, const uint8_t* data, int length) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!g_connected) {
        set_error("Not connected");
        return AITT_ERROR_CONNECTION;
    }
    
    if (!data || length <= 0) {
        set_error("Invalid parameters");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    
    // Use chipset-specific driver if available
    if (g_activeDriver && g_activeDriver->writeMemory) {
        int ret = g_activeDriver->writeMemory(address, data, length);
        LeaveCriticalSection(&g_lock);
        return ret;
    }
    
    // Default: accept write (placeholder)
    LeaveCriticalSection(&g_lock);
    return length;
}

// ============================================================================
// CHIPSET CONFIGURATION
// ============================================================================

int aitt_configure_chipset(const ChipsetConfig* config) {
    if (!g_initialized) {
        set_error("Not initialized");
        return AITT_ERROR_NOT_INITIALIZED;
    }
    
    if (!config || strlen(config->chipsetId) == 0) {
        set_error("Invalid configuration");
        return AITT_ERROR_INVALID_PARAM;
    }
    
    EnterCriticalSection(&g_lock);
    
    // Copy configuration
    memcpy(&g_currentConfig, config, sizeof(ChipsetConfig));
    
    // Find and activate driver for this chipset
    g_activeDriver = aitt_get_driver(config->chipsetId);
    
    if (!g_activeDriver) {
        char msg[128];
        snprintf(msg, sizeof(msg), "No driver for chipset: %s", config->chipsetId);
        set_error(msg);
        LeaveCriticalSection(&g_lock);
        return AITT_ERROR_UNSUPPORTED;
    }
    
    clear_error();
    LeaveCriticalSection(&g_lock);
    return AITT_OK;
}

// ============================================================================
// DRIVER REGISTRY
// ============================================================================

int aitt_register_driver(const ChipsetDriver* driver) {
    if (!driver || !driver->chipsetId) {
        return AITT_ERROR_INVALID_PARAM;
    }
    
    if (g_driverCount >= MAX_DRIVERS) {
        return AITT_ERROR_MEMORY;
    }
    
    g_drivers[g_driverCount++] = driver;
    return AITT_OK;
}

const ChipsetDriver* aitt_get_driver(const char* chipsetId) {
    if (!chipsetId) return NULL;
    
    for (int i = 0; i < g_driverCount; i++) {
        if (g_drivers[i] && strcmp(g_drivers[i]->chipsetId, chipsetId) == 0) {
            return g_drivers[i];
        }
    }
    
    return NULL;
}
