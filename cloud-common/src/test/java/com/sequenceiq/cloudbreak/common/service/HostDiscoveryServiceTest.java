package com.sequenceiq.cloudbreak.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HostDiscoveryServiceTest {

    @InjectMocks
    private HostDiscoveryService underTest;

    @Test
    void testGatewayFqdnGeneration() {
        String gatewayHostName = "datahub-master0";
        String defaultDomain = "env.xcu2-8y8x.dev.cldr.work";
        String gatewayFqdn = underTest.determineGatewayFqdn(gatewayHostName, defaultDomain);
        assertEquals(gatewayHostName + "." + defaultDomain, gatewayFqdn, "Generated Gateway FQDN should match");
    }
}
