package com.sequenceiq.freeipa.converter.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.dto.SidGeneration;
import com.sequenceiq.freeipa.entity.FreeIpa;

class FreeIpaServerRequestToFreeIpaConverterTest {
    private final FreeIpaServerRequestToFreeIpaConverter underTest = new FreeIpaServerRequestToFreeIpaConverter();

    @Test
    void convertShouldReturnFreeIpaWithProperValuesWhenGivenValidInput() {
        FreeIpaServerRequest source = new FreeIpaServerRequest();
        source.setAdminPassword("admin123");
        source.setDomain("example.com");
        source.setHostname("ipa.example.com");
        source.setAdminGroupName("admins");

        FreeIpa result = underTest.convert(source, "redhat7");

        assertNotNull(result);
        assertEquals("admin123", result.getAdminPassword());
        assertEquals("example.com", result.getDomain());
        assertEquals("ipa.example.com", result.getHostname());
        assertEquals("admins", result.getAdminGroupName());
        assertEquals(SidGeneration.DISABLED, result.getSidGeneration());
    }

    @Test
    void convertShouldReturnFreeIpaWithEnabledSidGenerationWhenGivenNonCentos7OsType() {
        FreeIpaServerRequest source = new FreeIpaServerRequest();

        FreeIpa result = underTest.convert(source, "REDHAT8");

        assertNotNull(result);
        assertEquals(SidGeneration.ENABLED, result.getSidGeneration());
    }
}