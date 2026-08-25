package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput.InstanceTypeSpecs;
import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class DatabaseInstanceTypeCapabilityValidatorTest {

    @InjectMocks
    private DatabaseInstanceTypeCapabilityValidator underTest;

    @Test
    void emptyAvailableTypesShouldSkipValidation() {
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.large", "db.m5.large", Architecture.X86_64, Map.of());
        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void nullAvailableTypesShouldSkipValidation() {
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.large", "db.m5.large", Architecture.X86_64, null);
        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void typeNotInAvailableTypesShouldThrow() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.m5.xlarge", new InstanceTypeSpecs(4, 16.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.nonexistent", "db.m5.large", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("not available in region"));
        assertTrue(ex.getMessage().contains("us-east-1"));
    }

    @Test
    void insufficientCpuShouldThrow() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.t3.small", new InstanceTypeSpecs(1, 2.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.t3.small", "db.m5.large", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("vCPU"));
        assertTrue(ex.getMessage().contains("less than the minimum"));
    }

    @Test
    void insufficientMemoryShouldThrow() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.t3.medium", new InstanceTypeSpecs(2, 4.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.t3.medium", "db.m5.large", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("memory"));
        assertTrue(ex.getMessage().contains("less than the minimum"));
    }

    @Test
    void architectureMismatchShouldThrow() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.m6g.large", new InstanceTypeSpecs(2, 8.0f, Architecture.ARM64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m6g.large", "db.m5.large", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("architecture"));
        assertTrue(ex.getMessage().contains("does not match"));
    }

    @Test
    void nullCpuDataShouldSkipCpuValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(null, 8.0f, Architecture.X86_64),
                "db.t3.small", new InstanceTypeSpecs(null, 2.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.t3.small", "db.m5.large", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("memory"));
    }

    @Test
    void nullMemoryDataShouldSkipMemoryValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, null, Architecture.X86_64),
                "db.m5.xlarge", new InstanceTypeSpecs(4, null, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.xlarge", "db.m5.large", Architecture.X86_64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void nullDesiredArchitectureShouldSkipArchitectureValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.m6g.large", new InstanceTypeSpecs(2, 8.0f, Architecture.ARM64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m6g.large", "db.m5.large", null, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void unknownArchitectureOnTypeShouldSkipArchitectureValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.custom", new InstanceTypeSpecs(4, 16.0f, Architecture.UNKNOWN));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.custom", "db.m5.large", Architecture.ARM64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void allChecksShouldPassForValidRequest() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.m5.xlarge", new InstanceTypeSpecs(4, 16.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.xlarge", "db.m5.large", Architecture.X86_64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void equalSpecsShouldPass() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.m5.large", new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64),
                "db.r5.large", new InstanceTypeSpecs(2, 16.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.r5.large", "db.m5.large", Architecture.X86_64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void nullDefaultTypeShouldSkipSpecValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.t3.micro", new InstanceTypeSpecs(1, 1.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.t3.micro", null, Architecture.X86_64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void defaultTypeNotInAvailableMapShouldSkipSpecValidation() {
        Map<String, InstanceTypeSpecs> available = Map.of(
                "db.t3.micro", new InstanceTypeSpecs(1, 1.0f, Architecture.X86_64));
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.t3.micro", "db.m5.large", Architecture.X86_64, available);

        assertDoesNotThrow(() -> underTest.validate(input));
    }

    @Test
    void regionAvailabilityErrorShouldTruncateWhenMoreThan20Types() {
        Map<String, InstanceTypeSpecs> available = new HashMap<>();
        for (int i = 1; i <= 25; i++) {
            available.put(String.format("db.m5.type%02d", i), new InstanceTypeSpecs(2, 8.0f, Architecture.X86_64));
        }
        DatabaseInstanceTypeValidationInput input = new DatabaseInstanceTypeValidationInput(
                "us-east-1", "db.m5.nonexistent", "db.m5.type01", Architecture.X86_64, available);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(input));
        assertTrue(ex.getMessage().contains("showing first 20 of 25"));
    }
}
