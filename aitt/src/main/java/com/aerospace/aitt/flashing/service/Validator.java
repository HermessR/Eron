package com.aerospace.aitt.flashing.service;

import com.aerospace.aitt.core.util.ChecksumUtil;
import com.aerospace.aitt.core.util.FileUtil;
import com.aerospace.aitt.flashing.model.SoftwareModule;
import com.aerospace.aitt.flashing.model.TargetProfile;
import com.aerospace.aitt.flashing.model.ValidationResult;
import com.aerospace.aitt.flashing.model.ValidationResult.ValidationError;
import com.aerospace.aitt.flashing.model.ValidationResult.ValidationErrorType;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Validates firmware modules before flashing.
 * Checks file existence, addresses, overlaps, and boundaries.
 */
public class Validator {
    
    // Forbidden address regions (e.g., null pointer region)
    private static final List<long[]> FORBIDDEN_REGIONS = List.of(
        new long[]{0x00000000, 0x00001000}  // First 4KB typically reserved
    );
    
    /**
     * Validates all modules against the target profile.
     */
    public ValidationResult validate(List<SoftwareModule> modules, TargetProfile target) {
        if (modules == null || modules.isEmpty()) {
            return ValidationResult.failure(
                ValidationErrorType.FILE_NOT_FOUND,
                "No firmware modules to validate"
            );
        }
        
        if (target == null) {
            return ValidationResult.failure(
                ValidationErrorType.INVALID_ADDRESS,
                "No target profile selected"
            );
        }
        
        List<ValidationError> errors = new ArrayList<>();
        
        // Validate each module
        for (SoftwareModule module : modules) {
            errors.addAll(validateModule(module, target));
        }
        
        // Check for overlaps between modules
        errors.addAll(checkOverlaps(modules));
        
        // Check for duplicate addresses
        errors.addAll(checkDuplicateAddresses(modules));
        
        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        
        return ValidationResult.failure(errors);
    }
    
    /**
     * Validates a single module.
     */
    private List<ValidationError> validateModule(SoftwareModule module, TargetProfile target) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Check file existence
        if (!Files.exists(module.filePath())) {
            errors.add(new ValidationError(
                ValidationErrorType.FILE_NOT_FOUND,
                "File not found: " + module.filePath(),
                module.name()
            ));
            return errors; // Can't continue validation without file
        }
        
        // Check file readability
        if (!Files.isReadable(module.filePath())) {
            errors.add(new ValidationError(
                ValidationErrorType.FILE_NOT_READABLE,
                "File not readable: " + module.filePath(),
                module.name()
            ));
            return errors;
        }
        
        // Check file format
        if (!FileUtil.isFirmwareFile(module.filePath())) {
            errors.add(new ValidationError(
                ValidationErrorType.INVALID_FORMAT,
                "Invalid firmware format. Expected .bin, .hex, .elf, or .srec",
                module.name()
            ));
        }
        
        // Check address is within flash bounds
        if (!target.containsAddress(module.flashAddress())) {
            errors.add(new ValidationError(
                ValidationErrorType.OUT_OF_BOUNDS,
                String.format("Address %s is outside flash region (%s - %s)",
                    module.formattedAddress(),
                    target.formattedFlashStart(),
                    target.formattedFlashEnd()),
                module.name()
            ));
        }
        
        // Check end address is within flash bounds
        if (!target.containsRegion(module.flashAddress(), module.size())) {
            errors.add(new ValidationError(
                ValidationErrorType.SIZE_EXCEEDED,
                String.format("Module extends beyond flash region (end: 0x%08X)",
                    module.endAddress()),
                module.name()
            ));
        }
        
        // Check for forbidden regions
        for (long[] forbidden : FORBIDDEN_REGIONS) {
            if (regionsOverlap(module.flashAddress(), module.size(), forbidden[0], forbidden[1] - forbidden[0])) {
                errors.add(new ValidationError(
                    ValidationErrorType.FORBIDDEN_ADDRESS,
                    String.format("Address 0x%08X overlaps forbidden region (0x%08X - 0x%08X)",
                        module.flashAddress(), forbidden[0], forbidden[1]),
                    module.name()
                ));
            }
        }
        
        // Verify checksum if stored
        if (module.checksum() != null && !module.checksum().isEmpty()) {
            try {
                String actualChecksum = ChecksumUtil.sha256(module.filePath());
                if (!actualChecksum.equalsIgnoreCase(module.checksum())) {
                    errors.add(new ValidationError(
                        ValidationErrorType.INVALID_CHECKSUM,
                        String.format("Checksum mismatch: expected %s, got %s",
                            module.checksum(), actualChecksum),
                        module.name()
                    ));
                }
            } catch (IOException e) {
                errors.add(new ValidationError(
                    ValidationErrorType.INVALID_CHECKSUM,
                    "Failed to verify checksum: " + e.getMessage(),
                    module.name()
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Checks for memory overlaps between modules.
     */
    private List<ValidationError> checkOverlaps(List<SoftwareModule> modules) {
        List<ValidationError> errors = new ArrayList<>();
        
        List<SoftwareModule> sorted = modules.stream()
            .sorted(Comparator.comparingLong(SoftwareModule::flashAddress))
            .toList();
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            SoftwareModule current = sorted.get(i);
            SoftwareModule next = sorted.get(i + 1);
            
            if (current.overlapsWith(next)) {
                errors.add(new ValidationError(
                    ValidationErrorType.ADDRESS_OVERLAP,
                    String.format("Memory overlap between '%s' (ends at 0x%08X) and '%s' (starts at %s)",
                        current.name(), current.endAddress(),
                        next.name(), next.formattedAddress()),
                    current.name()
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Checks for duplicate flash addresses.
     */
    private List<ValidationError> checkDuplicateAddresses(List<SoftwareModule> modules) {
        List<ValidationError> errors = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        
        for (SoftwareModule module : modules) {
            if (!seen.add(module.flashAddress())) {
                errors.add(new ValidationError(
                    ValidationErrorType.DUPLICATE_ADDRESS,
                    "Duplicate flash address: " + module.formattedAddress(),
                    module.name()
                ));
            }
        }
        
        return errors;
    }
    
    /**
     * Checks if two memory regions overlap.
     */
    private boolean regionsOverlap(long start1, long size1, long start2, long size2) {
        long end1 = start1 + size1;
        long end2 = start2 + size2;
        return start1 < end2 && start2 < end1;
    }
}
