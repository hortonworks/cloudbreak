package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NifiProxyHostsCollectorTest {

    @Test
    void testCollectKeepsOrderAndAddsLowerCaseVariants() {
        assertEquals(List.of("GW0.site", "gw0.site", "gw1.site", "Cluster.site", "cluster.site", "LB.elb.com", "lb.elb.com"),
                List.copyOf(NifiProxyHostsCollector.collect(Optional.of("GW0.site"), List.of("gw1.site"), "Cluster.site", Optional.of("LB.elb.com"))));
    }

    @Test
    void testCollectIgnoresOtherGatewaysWithoutPrimaryAndToleratesNull() {
        assertEquals(List.of("cluster.site"),
                List.copyOf(NifiProxyHostsCollector.collect(Optional.empty(), List.of("gw1.site"), "cluster.site", Optional.empty())));
        assertEquals(List.of("pending..."),
                List.copyOf(NifiProxyHostsCollector.collect(Optional.of("pending..."), null, null, Optional.empty())));
    }

    @Test
    void testCollectEmpty() {
        assertTrue(NifiProxyHostsCollector.collect(Optional.empty(), List.of(), "", Optional.of("")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.12.0.10000", "4.12.0.10000-1", "4.12.0.10000-238", "4.12.0.20000-231", "4.13.0.0-1"})
    void testProxyHostConfigRequired(String cfmVersion) {
        assertTrue(NifiProxyHostsCollector.isProxyHostConfigRequired(cfmVersion));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2.2.9.0-1", "4.11.0.0-100", "4.12.0.0-914", "4.12.0.401-1", "4.12.0.9999-999"})
    void testProxyHostConfigNotRequired(String cfmVersion) {
        assertFalse(NifiProxyHostsCollector.isProxyHostConfigRequired(cfmVersion));
    }
}
