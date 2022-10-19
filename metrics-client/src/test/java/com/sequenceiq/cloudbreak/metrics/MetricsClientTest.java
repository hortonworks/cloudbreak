package com.sequenceiq.cloudbreak.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsProcessorConfiguration;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsRecordProcessor;
import com.sequenceiq.cloudbreak.metrics.processor.MetricsRecordRequest;

import prometheus.Types;

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
        given(configuration.isStreamingEnabled()).willReturn(true);
        given(entitlementService.isComputeMonitoringEnabled(anyString())).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL, Optional.of(true));
        // THEN
        verify(metricsRecordProcessor, times(1)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithEntitlement() {
        // GIVEN
        given(configuration.isStreamingEnabled()).willReturn(true);
        given(entitlementService.isCdpSaasEnabled(anyString())).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL, Optional.of(true));
        // THEN
        verify(metricsRecordProcessor, times(1)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithNoPaasOrEntitlementSupport() {
        // GIVEN
        given(configuration.isStreamingEnabled()).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL, Optional.of(true));
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithDisabledGlobalMonitoring() {
        // GIVEN
        given(configuration.isStreamingEnabled()).willReturn(true);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL, Optional.of(true));
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }

    @Test
    public void testProcessStackStatusWithDisabledProcessor() {
        // GIVEN
        given(configuration.isStreamingEnabled()).willReturn(false);
        // WHEN
        underTest.processStackStatus(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL, Optional.of(true));
        // THEN
        verify(metricsRecordProcessor, times(0)).processRecord(any());
    }

    @Test
    public void testProcessRequestInOrder() {
        // GIVEN
        // WHEN
        MetricsRecordRequest request = underTest.processRequest(CRN, CLOUD_PLATFORM, STATUS, STATUS_ORDINAL,
                Crn.safeFromString(CRN), Optional.of(true), true);
        Types.TimeSeries timeSeries = request.getWriteRequest().getTimeseries(0);
        // THEN
        List<String> labels = List.of("__name__", "cluster_status", "cluster_type",
                "compute_monitoring_enabled", "platform", "resource_crn");
        Iterator<String> labelIterator = labels.iterator();
        for (int i = 0; i < labels.size(); i++) {
            assertEquals(timeSeries.getLabels(i).getName(), labelIterator.next());
        }
    }
}
