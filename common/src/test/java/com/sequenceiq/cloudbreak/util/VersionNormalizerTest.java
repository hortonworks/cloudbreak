package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VersionNormalizerTest {

    @Test
    void normalizeCdhVersion() {
        assertEquals("7.2.15", VersionNormalizer.normalizeCdhVersion("7.2.15-1.cdh7.2.15.p1.26792553"));
        assertEquals("7.0.1", VersionNormalizer.normalizeCdhVersion("7.0.1-Thiscanbeanything7.2.16"));
        assertEquals("invalid", VersionNormalizer.normalizeCdhVersion("invalid"));
    }
}