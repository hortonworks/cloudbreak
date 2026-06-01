package com.sequenceiq.freeipa.service.freeipa.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FreeIpaCertMongerConfigViewTest {
    @Test
    void testToMapWithEnrollTtls() {
        String ttls = "7776000, 604800, 86400, 3600";
        FreeIpaCertMongerConfigView view = FreeIpaCertMongerConfigView.builder()
                .withEnrollTtls(ttls)
                .build();
        Map<String, Object> map = view.toMap();
        assertEquals(ttls, map.get("enroll_ttls"));
    }

    @Test
    void testToMapWithNullEnrollTtls() {
        FreeIpaCertMongerConfigView view = FreeIpaCertMongerConfigView.builder().build();
        Map<String, Object> map = view.toMap();
        assertEquals("", map.get("enroll_ttls"));
    }

    @Test
    void testGetter() {
        String ttls = "7776000, 604800";
        FreeIpaCertMongerConfigView view = FreeIpaCertMongerConfigView.builder()
                .withEnrollTtls(ttls)
                .build();
        assertEquals(ttls, view.getEnrollTtls());
    }
}
