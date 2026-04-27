package com.aerospace.aitt.backend.hardware;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Concrete implementation of HardwareAbstractionLayer for MPC/S32K148 chipsets.
 * 
 * Provides chipset-specific address mappings and encoding formats.
 */
public class MPCHardwareImpl implements HardwareAbstractionLayer {
    
    private static final String CHIPSET_TYPE = "MPC5777M";
    
    // Logical name → hardware address mapping
    private static final Map<String, Long> LOGICAL_ADDRESSES = new HashMap<>();
    
    // Logical name → CMM format mapping
    private static final Map<String, String> LOGICAL_FORMATS = new HashMap<>();
    
    static {
        // MPC5777M register mappings
        // Control & Status registers (0x40000000 - 0x40000100)
        LOGICAL_ADDRESSES.put("temperature_sensor", 0x40000000L);
        LOGICAL_ADDRESSES.put("mode_flag", 0x40000004L);
        LOGICAL_ADDRESSES.put("calibration_data", 0x40000008L);
        LOGICAL_ADDRESSES.put("status_register", 0x40000010L);
        LOGICAL_ADDRESSES.put("error_code", 0x40000014L);
        LOGICAL_ADDRESSES.put("result_status", 0x40000018L);
        LOGICAL_ADDRESSES.put("voltage_output", 0x4000001CL);
        LOGICAL_ADDRESSES.put("current_input", 0x40000020L);
        
        // ADC Data registers (0x40000100 - 0x40000200)
        LOGICAL_ADDRESSES.put("adc_ch0_raw", 0x40000100L);
        LOGICAL_ADDRESSES.put("adc_ch1_raw", 0x40000104L);
        LOGICAL_ADDRESSES.put("adc_ch2_raw", 0x40000108L);
        LOGICAL_ADDRESSES.put("adc_ch3_raw", 0x4000010CL);
        
        // PWM Control registers (0x40000200 - 0x40000300)
        LOGICAL_ADDRESSES.put("pwm_duty_cycle", 0x40000200L);
        LOGICAL_ADDRESSES.put("pwm_frequency", 0x40000204L);
        LOGICAL_ADDRESSES.put("pwm_enable", 0x40000208L);
        
        // GPIO registers (0x40000300 - 0x40000400)
        LOGICAL_ADDRESSES.put("gpio_port_a", 0x40000300L);
        LOGICAL_ADDRESSES.put("gpio_port_b", 0x40000304L);
        
        // Format mappings
        LOGICAL_FORMATS.put("temperature_sensor", "sint32");
        LOGICAL_FORMATS.put("mode_flag", "bit");
        LOGICAL_FORMATS.put("calibration_data", "float32");
        LOGICAL_FORMATS.put("status_register", "uint32");
        LOGICAL_FORMATS.put("error_code", "sint32");
        LOGICAL_FORMATS.put("result_status", "bit");
        LOGICAL_FORMATS.put("voltage_output", "uint32");
        LOGICAL_FORMATS.put("current_input", "sint32");
        
        LOGICAL_FORMATS.put("adc_ch0_raw", "uint32");
        LOGICAL_FORMATS.put("adc_ch1_raw", "uint32");
        LOGICAL_FORMATS.put("adc_ch2_raw", "uint32");
        LOGICAL_FORMATS.put("adc_ch3_raw", "uint32");
        
        LOGICAL_FORMATS.put("pwm_duty_cycle", "uint32");
        LOGICAL_FORMATS.put("pwm_frequency", "uint32");
        LOGICAL_FORMATS.put("pwm_enable", "bit");
        
        LOGICAL_FORMATS.put("gpio_port_a", "uint32");
        LOGICAL_FORMATS.put("gpio_port_b", "uint32");
    }
    
    @Override
    public long resolveAddress(String logicalName, String dataType) throws IOException {
        if (logicalName == null || logicalName.trim().isEmpty()) {
            throw new IOException("Logical name cannot be null or empty");
        }
        
        Long address = LOGICAL_ADDRESSES.get(logicalName);
        if (address == null) {
            throw new IOException(
                "Unknown logical name: '" + logicalName + 
                "'. Available names: " + LOGICAL_ADDRESSES.keySet()
            );
        }
        
        return address;
    }
    
    @Override
    public String resolveFormat(String logicalName, String dataType) {
        if (logicalName == null) {
            return "sint32"; // default
        }
        
        String format = LOGICAL_FORMATS.get(logicalName);
        return format != null ? format : inferFormatFromDataType(dataType);
    }
    
    @Override
    public boolean isValidLogicalName(String logicalName) {
        return logicalName != null && LOGICAL_ADDRESSES.containsKey(logicalName);
    }
    
    @Override
    public String getChipsetType() {
        return CHIPSET_TYPE;
    }
    
    /**
     * Infers CMM format from UI data type when no explicit mapping exists.
     */
    private String inferFormatFromDataType(String dataType) {
        if (dataType == null) {
            return "sint32";
        }
        
        String normalized = dataType.toLowerCase();
        
        if (normalized.contains("boolean")) {
            return "bit";
        } else if (normalized.contains("signed")) {
            return "sint32";
        } else if (normalized.contains("unsigned")) {
            return "uint32";
        } else if (normalized.contains("float")) {
            return "float32";
        }
        
        return "sint32"; // default
    }
    
    /**
     * Gets all available logical names for UI autocomplete/validation.
     */
    public static java.util.Set<String> getAvailableLogicalNames() {
        return LOGICAL_ADDRESSES.keySet();
    }
}
