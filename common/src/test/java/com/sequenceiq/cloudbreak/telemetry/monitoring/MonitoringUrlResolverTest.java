package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonitoringUrlResolverTest {

    private MonitoringUrlResolver underTest;

    @BeforeEach
    public void setUp() {
        MonitoringConfiguration monitoringConfiguration = new MonitoringConfiguration();
        monitoringConfiguration.setRemoteWriteUrl("http://localhost/saas/$accountid");
        monitoringConfiguration.setPaasRemoteWriteUrl("http://localhost/$accountid");
        underTest = new MonitoringUrlResolver(monitoringConfiguration);
    }

    @Test
    public void shouldReturnPaasUrlWithAccountIdResolved() {
        String url = underTest.resolve("123", false);

        assertEquals("http://localhost/123", url);
    }

    @Test
    public void shouldReturnSaasUrlWithAccountIdResolved() {
        String url = underTest.resolve("123", true);

        assertEquals("http://localhost/saas/123", url);
    }

    @Test
    public void shouldReturnCustomUrlWithAccountIdResolved() {
        String url = underTest.resolve("123", "http://localhost/paas/$accountid");

        assertEquals("http://localhost/paas/123", url);
    }

    @Test
    public void shouldReturnCustomUrlWithOriginalAccountId() {
        String url = underTest.resolve("123", "http://localhost/paas/456");

        assertEquals("http://localhost/paas/456", url);
    }
}