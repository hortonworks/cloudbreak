package com.sequenceiq.cloudbreak.cloud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.Architecture;

@ExtendWith(MockitoExtension.class)
class ArchitectureTest {

    @Test
    void testFallbackNull() {
        Architecture result = Architecture.fromStringWithFallback(null);
        assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testValidationNull() {
        Architecture result = Architecture.fromStringWithValidation(null);
        assertNull(result);
    }

    @Test
    void testFallbackEmpty() {
        Architecture result = Architecture.fromStringWithFallback("");
        assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testValidationEmpty() {
        Architecture result = Architecture.fromStringWithValidation("");
        assertNull(result);
    }

    @Test
    void testFallbackX8664() {
        Architecture result = Architecture.fromStringWithFallback("x86_64");
        assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testValidationX8664() {
        Architecture result = Architecture.fromStringWithValidation("x86_64");
        assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testFallbackArm64() {
        Architecture result = Architecture.fromStringWithFallback("arm64");
        assertEquals(Architecture.ARM64, result);
    }

    @Test
    void testValidationArm64() {
        Architecture result = Architecture.fromStringWithValidation("arm64");
        assertEquals(Architecture.ARM64, result);
    }

    @Test
    void testFallbackUnknown() {
        Architecture result = Architecture.fromStringWithFallback("aarch64");
        assertEquals(Architecture.UNKOWN, result);
    }

    @Test
    void testValidationUnknown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Architecture.fromStringWithValidation("aarch64"));
        assertEquals("Architecture 'aarch64' is not supported", exception.getMessage());
    }
}
