package com.sequenceiq.cloudbreak.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HostDiscoveryServiceTest {

    @InjectMocks
    private HostDiscoveryService underTest;

    @Test
    public void testStackDomainGeneration() {
        String gatewayHostName = "datahub-master0";
        String defaultDomain = "env.xcu2-8y8x.dev.cldr.work";
        String stackDomain = underTest.determineDefaultDomainForStack(gatewayHostName, defaultDomain);
        assertEquals(gatewayHostName + "." + defaultDomain, stackDomain, "Generated Stack Domain should match");
    }
}
