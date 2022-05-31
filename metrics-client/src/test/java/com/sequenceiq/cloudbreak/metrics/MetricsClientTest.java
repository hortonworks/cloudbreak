package com.sequenceiq.cloudbreak.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsProcessorConfiguration;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsRecordProcessor;

@ExtendWith(MockitoExtension.class)
public class MetricsClientTest {

    private static final String CRN = "crn:cdp:freeipa:us-west-1:cloudera:freeipa:4428e540-a878-42b1-a1d4-91747322d8b6";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String STATUS = "AVAILABLE";

    private static final Integer STATUS_ORDINAL = 2;

    @InjectMocks
    private MetricsClient underTest;

    @Mock
    private MetricsRecordProcessor metricsRecordProcessor;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private MetricsProcessorConfiguration configuration;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        given(metricsRecordProcessor.getConfiguration()).willReturn(configuration);
        underTest = new MetricsClient(metricsRecordProcessor, entitlementService);
    }

    @Test
    public void testProcessStackStatus() {
        // GIVEN
        given(configuration.isEnabled()).willReturn(true);
        given(configuration.isComputeMonitoringSupported()).willReturn(true);
        given(configuration.isPaasSupported()).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL);
        // THEN
        verify(metricsRecordProcessor, times(1)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithEntitlement() {
        // GIVEN
        given(configuration.isEnabled()).willReturn(true);
        given(configuration.isComputeMonitoringSupported()).willReturn(true);
        given(configuration.isPaasSupported()).willReturn(false);
        given(entitlementService.isCdpSaasEnabled(anyString())).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL);
        // THEN
        verify(metricsRecordProcessor, times(1)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithNoPaasOrEntitlementSupport() {
        // GIVEN
        given(configuration.isEnabled()).willReturn(true);
        given(configuration.isComputeMonitoringSupported()).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL);
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithDisabledGlobalMonitoring() {
        // GIVEN
        given(configuration.isEnabled()).willReturn(true);
        given(configuration.isComputeMonitoringSupported()).willReturn(false);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL);
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithDisabledProcessor() {
        // GIVEN
        given(configuration.isEnabled()).willReturn(false);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL);
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }
}