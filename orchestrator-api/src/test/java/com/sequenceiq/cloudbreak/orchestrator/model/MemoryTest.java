package com.sequenceiq.cloudbreak.orchestrator.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoryTest {

    @Test
    public void testKiloByteConversion() {
        assertEquals(1024, Memory.of(1, "kB").getValueInBytes());
    }

    @Test
    public void testMegaByteConversion() {
        assertEquals(1048576, Memory.of(1, "MB").getValueInBytes());
    }

    @Test
    public void testGigaByteConversion() {
        assertEquals(1073741824, Memory.of(1, "GB").getValueInBytes());
    }

    @Test
    public void testTeraByteConversion() {
        assertEquals(1099511627776L, Memory.of(1, "TB").getValueInBytes());
    }
}