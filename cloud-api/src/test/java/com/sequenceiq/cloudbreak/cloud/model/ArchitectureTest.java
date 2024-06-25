package com.sequenceiq.cloudbreak.cloud.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchitectureTest {

    @Test
    void testNull() {
        Architecture result = Architecture.fromString(null);
        Assertions.assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testEmpty() {
        Architecture result = Architecture.fromString("");
        Assertions.assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testX8664() {
        Architecture result = Architecture.fromString("x86_64");
        Assertions.assertEquals(Architecture.X86_64, result);
    }

    @Test
    void testArm64() {
        Architecture result = Architecture.fromString("arm64");
        Assertions.assertEquals(Architecture.ARM64, result);
    }

    @Test
    void testUnknown() {
        Architecture result = Architecture.fromString("aarch64");
        Assertions.assertEquals(Architecture.UNKOWN, result);
    }
}
