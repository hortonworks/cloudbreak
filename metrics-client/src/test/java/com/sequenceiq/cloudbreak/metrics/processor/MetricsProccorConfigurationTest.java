package com.sequenceiq.cloudbreak.metrics.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@ExtendWith(MockitoExtension.class)
public class MetricsProccorConfigurationTest {

    private MetricsProcessorConfiguration underTest;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new MetricsProcessorConfiguration(monitoringConfiguration, true, 0, 0, 0);
    }

    @Test
    public void testGetRemoteWriteUrl() {
        // GIVEN
        given(monitoringConfiguration.getRemoteWriteUrl()).willReturn("http://myremotewriteurl");
        given(monitoringConfiguration.getRemoteWriteInternalUrl()).willReturn(null);
        // WHEN
        String result = underTest.getRemoteWriteUrl();
        // THEN
        assertEquals("http://myremotewriteurl", result);
    }

    @Test
    public void testGetRemoteWriteUrlEmpty() {
        // GIVEN
        given(monitoringConfiguration.getRemoteWriteUrl()).willReturn(null);
        given(monitoringConfiguration.getRemoteWriteInternalUrl()).willReturn(null);
        // WHEN
        String result = underTest.getRemoteWriteUrl();
        // THEN
        assertNull(result);
    }

    @Test
    public void testGetRemoteWriteUrlWithInternalUrl() {
        // GIVEN
        given(monitoringConfiguration.getRemoteWriteInternalUrl()).willReturn("http://myremotewriteinternalurl");
        // WHEN
        String result = underTest.getRemoteWriteUrl();
        // THEN
        assertEquals("http://myremotewriteinternalurl", result);
    }
}
