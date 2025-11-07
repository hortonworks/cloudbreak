package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;

import org.junit.jupiter.api.Test;

class DistroxEnabledInstanceTypesTest {

    @Test
    void testSplitInstanceListRemovesEmptyEntries() {
        Set<String> x86 = DistroxEnabledInstanceTypes.AWS_ENABLED_X86_TYPES_LIST;
        assertFalse(x86.contains(""));
        assertEquals(x86.size(), 329);

        Set<String> graviton = DistroxEnabledInstanceTypes.AWS_ENABLED_ARM64_TYPES_LIST;
        assertFalse(graviton.contains(""));
        assertEquals(graviton.size(), 173);
    }
}