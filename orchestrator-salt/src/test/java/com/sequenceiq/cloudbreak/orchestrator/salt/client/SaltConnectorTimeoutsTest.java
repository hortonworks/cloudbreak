package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

class SaltConnectorTimeoutsTest {

    @Test
    void testDefaultsMatchTheValuesTheConnectorHardCodedBefore() {
        SaltConnectorTimeouts defaults = SaltConnectorTimeouts.defaults();

        assertEquals(20_000, defaults.connectTimeoutMs());
        assertEquals(OptionalInt.empty(), defaults.readTimeoutMs());
        assertEquals(90_000, defaults.proxyTimeoutMs());
    }

    @Test
    void testConnectAndReadTimeoutKeepsTheDefaultProxyTimeout() {
        assertEquals(new SaltConnectorTimeouts(1000, OptionalInt.of(2000), 90_000), SaltConnectorTimeouts.of(1000, 2000));
    }

    @Test
    void testAllTimeoutsCanBeCustomized() {
        assertEquals(new SaltConnectorTimeouts(1000, OptionalInt.of(2000), 3000), SaltConnectorTimeouts.of(1000, 2000, 3000));
    }
}
