package com.aerospace.aitt.backend.hardware;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * CMMScriptGenerator - Generates Trace32 CMM test scripts for MPC chipset validation.
 * 
 * Integrates with AITT's abstraction layer to resolve hardware addresses and
 * generates type-aware test scripts ready for Trace32 execution.
 * 
 * Architecture:
 *   1. UI provides logical input/output definitions
 *   2. This class reads template and populates with resolved hardware addresses
 *   3. Generates .cmm file ready for Trace32 CLI execution
 *   4. External system compares captured vs. expected outputs
 * 
 * @author AITT Backend
 */
public class CMMScriptGenerator {
    
    private static final String TEMPLATE_FILE = "aitt/native/templates/mpc_validation_template.cmm";
    private static final String OUTPUT_DIR = "aitt/output";
    private static final int MAX_INPUTS = 8;
    private static final int MAX_OUTPUTS = 8;
    
    private final HardwareAbstractionLayer hardwareLayer;
    
    public CMMScriptGenerator(HardwareAbstractionLayer hardwareLayer) {
        this.hardwareLayer = hardwareLayer;
    }
    
    /**
     * Generates a CMM script from user-provided test definition.
     * 
     * @param testDefinition Contains UI inputs, outputs, breakpoint name
     * @return Path to generated .cmm file
     * @throws IOException if template or output operations fail
     */
    public Path generateScript(TestDefinition testDefinition) throws IOException {
        
        // Load template
        String template = loadTemplate();
        String script = template;
        
        // Populate inputs section
        script = populateInputs(script, testDefinition.getInputs());
        
        // Populate outputs section
        script = populateOutputs(script, testDefinition.getOutputs());
        
        // Populate breakpoint
        script = script.replace(
            "&breakpoint_name=\"UserFunction_ValidateChipset\"",
            "&breakpoint_name=\"" + testDefinition.getBreakpointName() + "\""
        );
        
        // Save to timestamped file
        Path outputPath = saveScript(script, testDefinition.getTestName());
        
        return outputPath;
    }
    
    /**
     * Populates input placeholders in template with resolved hardware addresses.
     */
    private String populateInputs(String script, List<TestInput> inputs) 
            throws IOException {
        
        if (inputs == null || inputs.isEmpty()) {
            return script;
        }
        
        if (inputs.size() > MAX_INPUTS) {
            throw new IllegalArgumentException(
                "Maximum " + MAX_INPUTS + " inputs supported, got " + inputs.size()
            );
        }
        
        for (int idx = 0; idx < inputs.size(); idx++) {
            TestInput input = inputs.get(idx);
            int index = idx + 1;
            
            // Resolve hardware address from logical name
            long hardwareAddress = hardwareLayer.resolveAddress(
                input.getLogicalName(), 
                input.getDataType()
            );
            
            // Determine CMM format based on data type
            String cmmFormat = hardwareLayer.resolveFormat(input.getLogicalName(), input.getDataType());
            
            // Replace placeholders for this input
            script = script.replace(
                "&INPUT_" + index + "_NAME=\"temperature_sensor\"",
                "&INPUT_" + index + "_NAME=\"" + input.getLogicalName() + "\""
            );
            
            script = script.replace(
                "&INPUT_" + index + "_TYPE=\"int\"",
                "&INPUT_" + index + "_TYPE=\"" + input.getDataType() + "\""
            );
            
            script = script.replace(
                "&INPUT_" + index + "_MODE=\"RW\"",
                "&INPUT_" + index + "_MODE=\"" + input.getMode() + "\""
            );
            
            script = script.replace(
                "&INPUT_" + index + "_VALUE=1250",
                "&INPUT_" + index + "_VALUE=" + input.getValue()
            );
            
            script = script.replace(
                "&INPUT_" + index + "_ADDRESS=0x40000000",
                "&INPUT_" + index + "_ADDRESS=0x" + String.format("%08X", hardwareAddress)
            );
            
            script = script.replace(
                "&INPUT_" + index + "_FORMAT=\"sint32\"",
                "&INPUT_" + index + "_FORMAT=\"" + cmmFormat + "\""
            );
        }
        
        return script;
    }
    
    /**
     * Populates output placeholders in template with resolved hardware addresses.
     */
    private String populateOutputs(String script, List<TestOutput> outputs) 
            throws IOException {
        
        if (outputs == null || outputs.isEmpty()) {
            return script;
        }
        
        if (outputs.size() > MAX_OUTPUTS) {
            throw new IllegalArgumentException(
                "Maximum " + MAX_OUTPUTS + " outputs supported, got " + outputs.size()
            );
        }
        
        for (int idx = 0; idx < outputs.size(); idx++) {
            TestOutput output = outputs.get(idx);
            int index = idx + 1;
            
            // Resolve hardware address
            long hardwareAddress = hardwareLayer.resolveAddress(
                output.getLogicalName(),
                output.getDataType()
            );
            
            String cmmFormat = hardwareLayer.resolveFormat(output.getLogicalName(), output.getDataType());
            
            script = script.replace(
                "&OUTPUT_" + index + "_NAME=\"status_register\"",
                "&OUTPUT_" + index + "_NAME=\"" + output.getLogicalName() + "\""
            );
            
            script = script.replace(
                "&OUTPUT_" + index + "_TYPE=\"int\"",
                "&OUTPUT_" + index + "_TYPE=\"" + output.getDataType() + "\""
            );
            
            script = script.replace(
                "&OUTPUT_" + index + "_MODE=\"R\"",
                "&OUTPUT_" + index + "_MODE=\"" + output.getMode() + "\""
            );
            
            script = script.replace(
                "&OUTPUT_" + index + "_ADDRESS=0x40000010",
                "&OUTPUT_" + index + "_ADDRESS=0x" + String.format("%08X", hardwareAddress)
            );
            
            script = script.replace(
                "&OUTPUT_" + index + "_FORMAT=\"uint32\"",
                "&OUTPUT_" + index + "_FORMAT=\"" + cmmFormat + "\""
            );
        }
        
        return script;
    }
    
    /**
     * Loads CMM template from file.
     */
    private String loadTemplate() throws IOException {
        Path templatePath = Paths.get(TEMPLATE_FILE);
        return new String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8);
    }
    
    /**
     * Saves generated script to timestamped file.
     */
    private Path saveScript(String script, String testName) throws IOException {
        // Create output directory if needed
        Path outputPath = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputPath);
        
        // Generate timestamped filename
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format("test_%s_%s.cmm", testName, timestamp);
        
        Path scriptPath = outputPath.resolve(filename);
        Files.write(scriptPath, script.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("CMM script generated: " + scriptPath);
        return scriptPath;
    }
    
    // ========================================================================
    // Inner Classes
    // ========================================================================
    
    /**
     * Test definition containing inputs, outputs, and execution parameters.
     */
    public static class TestDefinition {
        private String testName;
        private List<TestInput> inputs;
        private List<TestOutput> outputs;
        private String breakpointName;
        
        public TestDefinition(String testName, 
                            List<TestInput> inputs,
                            List<TestOutput> outputs,
                            String breakpointName) {
            this.testName = testName;
            this.inputs = inputs;
            this.outputs = outputs;
            this.breakpointName = breakpointName;
        }
        
        public String getTestName() { return testName; }
        public List<TestInput> getInputs() { return inputs; }
        public List<TestOutput> getOutputs() { return outputs; }
        public String getBreakpointName() { return breakpointName; }
    }
    
    /**
     * Test input definition (from UI layer).
     */
    public static class TestInput {
        private String logicalName;      // e.g., "temperature_sensor"
        private String dataType;         // e.g., "int (signed)", "boolean", "float"
        private String mode;             // "R", "W", or "RW"
        private String value;            // Test value: "1250", "true", "2.5"
        
        public TestInput(String logicalName, String dataType, String mode, String value) {
            this.logicalName = logicalName;
            this.dataType = dataType;
            this.mode = mode;
            this.value = value;
        }
        
        public String getLogicalName() { return logicalName; }
        public String getDataType() { return dataType; }
        public String getMode() { return mode; }
        public String getValue() { return value; }
        
        @Override
        public String toString() {
            return String.format(
                "TestInput{name='%s', type='%s', mode='%s', value='%s'}",
                logicalName, dataType, mode, value
            );
        }
    }
    
    /**
     * Test output definition (from UI layer).
     * Post-processor will compare captured values against expected values.
     */
    public static class TestOutput {
        private String logicalName;      // e.g., "status_register"
        private String dataType;         // e.g., "int (unsigned)"
        private String mode;             // "R", "W", or "RW"
        
        public TestOutput(String logicalName, String dataType, String mode) {
            this.logicalName = logicalName;
            this.dataType = dataType;
            this.mode = mode;
        }
        
        public String getLogicalName() { return logicalName; }
        public String getDataType() { return dataType; }
        public String getMode() { return mode; }
        
        @Override
        public String toString() {
            return String.format(
                "TestOutput{name='%s', type='%s', mode='%s'}",
                logicalName, dataType, mode
            );
        }
    }
    
    /**
     * Example usage demonstrating integration with AITT UI.
     */
    public static void main(String[] args) throws IOException {
        // Create hardware abstraction layer
        HardwareAbstractionLayer hardwareLayer = new MPCHardwareImpl();
        
        // Initialize generator
        CMMScriptGenerator generator = new CMMScriptGenerator(hardwareLayer);
        
        // Define test inputs (from UI)
        List<TestInput> inputs = Arrays.asList(
            new TestInput("temperature_sensor", "int (signed)", "RW", "1250"),
            new TestInput("mode_flag", "boolean", "W", "1"),
            new TestInput("calibration_data", "float", "RW", "2.5")
        );
        
        // Define test outputs (for post-processing)
        List<TestOutput> outputs = Arrays.asList(
            new TestOutput("status_register", "int (unsigned)", "R"),
            new TestOutput("error_code", "int (signed)", "R"),
            new TestOutput("result_status", "boolean", "R")
        );
        
        // Create test definition
        TestDefinition testDef = new TestDefinition(
            "ChipsetValidation_001",
            inputs,
            outputs,
            "UserFunction_ValidateChipset"
        );
        
        // Generate CMM script
        Path scriptPath = generator.generateScript(testDef);
        System.out.println("Generated CMM script: " + scriptPath);
        
        // Script can now be executed via Trace32 CLI:
        // T32.exe -c scriptPath
    }
}
