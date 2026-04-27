# AITT CMM Test Script Generation Guide

## Overview

The `mpc_validation_template.cmm` is a **reusable Trace32 CMM template** that implements the MPC chipset validation framework. It's designed to be populated with user inputs from the AITT UI and executed on hardware.

## Architecture Integration

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. UI Layer (AITT JavaFX)                                       │
│    User defines inputs/outputs with logical names and types     │
│    (No hardware addresses visible)                              │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Abstraction Layer (Pre-Processor)                            │
│    Resolves:                                                    │
│    - Hardware addresses from logical names                      │
│    - Encoding formats per data type                             │
│    - Generates CMM script with resolved values                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Trace32 Execution Layer (THIS CMM SCRIPT)                    │
│    ✓ Injects inputs via Data.Set                               │
│    ✓ Captures outputs via Data.*                               │
│    ✓ Controls breakpoints and execution flow                   │
│    ✓ Logs all operations                                        │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Post-Processing Layer (External System)                      │
│    Compares captured values against expected values             │
│    (Comparison logic NOT in CMM)                                │
└─────────────────────────────────────────────────────────────────┘
```

## Template Customization

### Step 1: UI Input Collection

Users provide via AITT UI:

| Field | Type | Example | Purpose |
|-------|------|---------|---------|
| Input Name | String | `temperature_sensor` | Logical identifier |
| Data Type | Dropdown | `int (signed)` | Value representation |
| Mode | Radio | `R/W/RW` | Read/Write capability |
| Test Value | Number/Boolean | `1250` | Value to inject |

### Step 2: Abstraction Layer Processing

The pre-processor resolves:

```json
{
  "input_name": "temperature_sensor",
  "ui_type": "int (signed)",
  "ui_mode": "RW",
  "ui_value": 1250,
  
  "resolved_hardware_address": "0x40000000",
  "resolved_format": "sint32",
  "resolved_encoding": "signed_32bit"
}
```

### Step 3: CMM Script Generation

Replace template placeholders:

```cmm
; BEFORE (template):
&INPUT_1_NAME="PLACEHOLDER"
&INPUT_1_ADDRESS=0xDEADBEEF

; AFTER (generated):
&INPUT_1_NAME="temperature_sensor"
&INPUT_1_ADDRESS=0x40000000
```

### Step 4: Trace32 Execution

The CMM script:
- ✓ Injects values
- ✓ Triggers breakpoint
- ✓ Captures outputs
- ✓ Logs results

### Step 5: Post-Processing

External system reads logs and compares:

```
Captured: &OUTPUT_1_CAPTURED = 1280
Expected: 1280 (from UI)
Result:   PASS ✓
```

---

## Supported Data Types

### boolean
- **CMM Format**: `%Byte` with bit masking (`value&0x1`)
- **Injection**: 0 or 1
- **Example**: `Data.Set 0x40000004 %Byte 1`
- **Readback**: `Data.Byte(0x40000004)&0x1`

### int (signed)
- **CMM Format**: `%Long` (signed 32-bit)
- **Injection**: Raw signed value
- **Example**: `Data.Set 0x40000000 %Long -42`
- **Readback**: `Data.Long(0x40000000)`

### int (unsigned)
- **CMM Format**: `%Long` (unsigned 32-bit)
- **Injection**: Raw unsigned value
- **Example**: `Data.Set 0x40000000 %Long 1250`
- **Readback**: `Data.Long(0x40000000)`

### float
- **CMM Format**: `%Float` (IEEE 754 single precision)
- **Injection**: Decimal value
- **Example**: `Data.Set 0x40000008 %Float 2.5`
- **Readback**: `Data.Float(0x40000008)`

### read-only
- **Mode**: `R`
- **Injection**: ❌ Skipped (conditional: IF "MODE"=="R" SKIP)
- **Readback**: ✓ Allowed

### write-only
- **Mode**: `W`
- **Injection**: ✓ Allowed
- **Readback**: ❌ Skipped (no validation in script)

### read-write
- **Mode**: `RW`
- **Injection**: ✓ Allowed
- **Readback**: ✓ Allowed

---

## CMM Script Sections Explained

### Section 1: Initialization
- Opens log file with timestamped name
- Initializes chip type detection
- Prints header

### Section 2: Input Definitions
- **8 input slots** defined (INDEX 1-8 in actual implementation)
- Each input has:
  - Logical name
  - Data type (boolean | int | float | R/O | W/O | RW)
  - Mode (R | W | RW)
  - Test value
  - Hardware address (resolved)
  - Format specification

### Section 3: Output Definitions
- **8 output slots** defined
- Same structure as inputs
- Post-processor will use captured values

### Section 4: Breakpoint Configuration
- Sets breakpoint at target function (USER INPUT)
- Clears any prior breakpoints
- Uses `/Onchip` qualifier

### Section 5: Input Injection
- **Type-aware conditional logic**
- For each input:
  - Check mode (skip if read-only)
  - Choose format (`%Byte`, `%Long`, `%Float`)
  - Execute `Data.Set` command
  - Log operation

### Section 6: Execution Start
- Resets CPU
- `Go` command starts execution
- `WAIT Break` pauses at breakpoint
- Logs timing

### Section 7: Output Capture
- **Type-aware conditional readback**
- For each output:
  - Check mode (skip if write-only)
  - Choose read function (`Data.Byte()`, `Data.Long()`, `Data.Float()`)
  - Store captured value
  - Log result

### Section 8: Completion
- Prints summary of all inputs/outputs
- Logs timing and breakpoint name
- Notes post-processing stage
- Closes file and breaks execution

---

## Integration with AITT

### For Java Backend (Pre-Processor):

```java
class CMMScriptGenerator {
    public String generateScript(List<TestInput> inputs, 
                                 List<TestOutput> outputs,
                                 String breakpointName) {
        String template = readTemplateFile("mpc_validation_template.cmm");
        String script = template;
        
        // Populate inputs
        for (int i = 0; i < inputs.size(); i++) {
            TestInput input = inputs.get(i);
            String address = resolveAddress(input.logicalName);
            String format = resolveFormat(input.dataType);
            
            script = script.replace(
                "&INPUT_" + (i+1) + "_NAME",
                "\"" + input.logicalName + "\""
            );
            script = script.replace(
                "&INPUT_" + (i+1) + "_ADDRESS",
                address
            );
            script = script.replace(
                "&INPUT_" + (i+1) + "_FORMAT",
                format
            );
            // ... repeat for VALUE, TYPE, MODE
        }
        
        // Populate outputs (similar)
        // Populate breakpoint
        // ...
        
        return script;
    }
}
```

### For UI (Input Collection):

```java
// RemoteController.java or similar
@FXML
private void onAddInput() {
    String name = inputNameField.getText();
    String type = typeCombo.getValue();      // "int (signed)", "boolean", etc.
    String mode = modeRadio.getValue();      // "R", "W", "RW"
    String value = valueField.getText();
    
    TestInput input = new TestInput(name, type, mode, value);
    inputTable.getItems().add(input);
}

@FXML
private void onGenerateAndExecute() {
    List<TestInput> inputs = inputTable.getItems();
    List<TestOutput> outputs = outputTable.getItems();
    String breakpointName = breakpointField.getText();
    
    String cmmScript = scriptGenerator.generateScript(
        inputs, outputs, breakpointName
    );
    
    // Write to .cmm file
    Files.write(Paths.get("test_generated.cmm"), cmmScript.getBytes());
    
    // Execute via Trace32 CLI
    processBuilder.command("T32", "test_generated.cmm");
    processBuilder.start();
}
```

---

## Usage Workflow

1. **User opens AITT UI** (e.g., "Remote Testing" module)
2. **User defines test inputs** (via table or form):
   - sensed_temperature, int (signed), RW, 1250
   - mode_flag, boolean, W, 1
   - calibration_data, float, RW, 2.5
3. **User defines expected outputs** (for post-processing):
   - status_register, int (unsigned), R
   - error_code, int (signed), R
   - result_status, boolean, R
4. **User selects breakpoint** (e.g., "UserFunction_ValidateChipset")
5. **Backend (Java) generates CMM script**:
   - Reads template
   - Resolves hardware addresses via abstraction layer
   - Populates placeholders
   - Saves to `.cmm` file
6. **Trace32 executes CMM script**:
   - Injects inputs → Go to breakpoint → Captures outputs → Logs result
7. **External post-processor compares**:
   - Reads log file
   - Compares captured vs. expected
   - Generates PASS/FAIL report

---

## Example Generated Script (Partial)

After pre-processor resolves addresses:

```cmm
&INPUT_1_NAME="temperature_sensor"
&INPUT_1_TYPE="int"
&INPUT_1_MODE="RW"
&INPUT_1_VALUE=1250
&INPUT_1_ADDRESS=0x40000000
&INPUT_1_FORMAT="sint32"

; In injection phase:
IF ("RW"=="W")||("RW"=="RW")
(
  PRINT "[INPUT_1] Injecting temperature_sensor = 1250 (format: sint32)"
  Data.Set 0x40000000 %Long 1250
  PRINT "  ✓ Injection complete, address: 0x40000000"
)
```

---

## Chipset-Specific Notes

### MPC5777M
- Register base: Typically `0x40000000`
- Max clock cycles for operation: ~100
- Recommended breakpoint location: Post-ADC conversion handler

### S32K148
- Register base: Typically `0x40000000`
- Flash memory: 0x0 - 0x7FFFF
- RAM: 0x1FFF0000 - 0x2000FFFF

---

## Debugging CMM Scripts

If test fails to execute:

1. **Check breakpoint exists**:
   ```cmm
   Break.List
   ```

2. **Verify hardware addresses**:
   ```cmm
   PRINT "Address: 0x" + FORMAT.HEX(8., &INPUT_1_ADDRESS)
   ```

3. **Monitor CPU state**:
   ```cmm
   REGISTER
   ```

4. **Check Data.Set permissions**:
   - Some addresses may be read-only or protected
   - Data.* commands fail silently if address is invalid

---

## Next Steps

1. Integrate template path into AITT backend
2. Implement address resolution in abstraction layer
3. Create UI form for input/output definition
4. Deploy CMM script generator class
5. Add Trace32 CLI integration
