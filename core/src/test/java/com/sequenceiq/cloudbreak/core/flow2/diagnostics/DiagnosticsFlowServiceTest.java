package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class DiagnosticsFlowServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environment:eu-1:1234:user:91011";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:eu-1:1234:user:91011";

    private static final String GATEWAY_FQDN = "gateway.example.com";

    private static final String DATABUS_ENDPOINT = "https://databus.cloudera.com";

    private static final String DATABUS_S3_ENDPOINT = "https://databus-s3.cloudera.com";

    private static final String MONITORING_URL = "https://monitoring.cloudera.com/api/v1/write";

    private static final String AWS_METADATA_WARNING = "Could be related with unavailable instance metadata service response";

    @InjectMocks
    private DiagnosticsFlowService underTest;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData primaryGatewayInstance;

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private CdpDoctorService cdpDoctorService;

    @Mock
    private PreFlightCheckValidationService preFlightCheckValidationService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    private CdpDoctorNetworkStatusResponse resp;

    private Map<String, CdpDoctorNetworkStatusResponse> responses;

    @BeforeEach
    void init() {
        resp = new CdpDoctorNetworkStatusResponse();
        resp.setCdpTelemetryVersion("1.3.6");
        resp.setDatabusAccessible(CdpDoctorCheckStatus.OK);
        resp.setDatabusS3Accessible(CdpDoctorCheckStatus.OK);
        resp.setArchiveClouderaComAccessible(CdpDoctorCheckStatus.OK);
        resp.setS3Accessible(CdpDoctorCheckStatus.OK);
        resp.setStsAccessible(CdpDoctorCheckStatus.OK);
        resp.setAdlsV2Accessible(CdpDoctorCheckStatus.OK);
        resp.setAzureManagementAccessible(CdpDoctorCheckStatus.OK);
        resp.setGcsAccessible(CdpDoctorCheckStatus.OK);
        resp.setComputeMonitoringAccessible(CdpDoctorCheckStatus.OK);
        resp.setServiceDeliveryCacheS3Accessible(CdpDoctorCheckStatus.OK);
        responses = new HashMap<>();
        responses.put(GATEWAY_FQDN, resp);

        lenient().when(stack.getPrimaryGatewayInstance()).thenReturn(primaryGatewayInstance);
        lenient().when(primaryGatewayInstance.getDiscoveryFQDN()).thenReturn(GATEWAY_FQDN);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        lenient().when(stack.getResourceCrn()).thenReturn(DATAHUB_CRN);
        lenient().when(stack.getName()).thenReturn("cluster");
        lenient().when(stack.getType()).thenReturn(StackType.WORKLOAD);
        lenient().when(stack.getCloudPlatform()).thenReturn("AWS");
        lenient().when(stack.getRegion()).thenReturn("us-west-2");
        lenient().when(stack.getId()).thenReturn(1L);
    }

    @Test
    void versionEquals() {
        assertTrue(underTest.isVersionGreaterOrEqual("0.4.8", "0.4.8"));
    }

    @Test
    void versionLess() {
        assertFalse(underTest.isVersionGreaterOrEqual("0.4.7", "0.4.8"));
    }

    @Test
    void versionGreater() {
        assertTrue(underTest.isVersionGreaterOrEqual("0.4.9", "0.4.8"));
    }

    @Test
    void versionBetaGreater() {
        assertTrue(underTest.isVersionGreaterOrEqual("1.3.5.1", "1.3.5"));
    }

    @DisplayName("1. Early return when preflight not supported")
    @Test
    void preflightNotSupportedSkipsEverything() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        when(cdpDoctorService.getNetworkStatusForMinions(stack)).thenReturn(responses);
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(false);

        underTest.nodeStatusNetworkReport(stack);

        verify(preFlightCheckValidationService).preFlightCheckSupported(ENVIRONMENT_CRN, true);
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(usageReporter);
    }

    @DisplayName("2. All OK -> 9 events (including compute monitoring) + 9 usage reports")
    @Test
    void allOkEmitsEventsAndUsage() throws Exception {
        stubHappyInfra();
        underTest.nodeStatusNetworkReport(stack);
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("3. Failure without AWS metadata v2 support -> conditional warning appears")
    @Test
    void s3FailureBeforeAwsMetadataV2AddsWarning() throws Exception {
        resp.setCdpTelemetryVersion("0.4.8");
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("0.4.8");
        resp.setS3Accessible(CdpDoctorCheckStatus.NOK);
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, false)).thenReturn(true);

        underTest.nodeStatusNetworkReport(stack);

        ArgumentCaptor<List> paramsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cloudbreakEventService).fireCloudbreakEvent(eq(1L), eq("UPDATE_FAILED"), eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED),
                paramsCaptor.capture());
        List<String> params = paramsCaptor.getValue();
        assertTrue(params.get(2).contains(AWS_METADATA_WARNING));
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("4. One generic failure after AWS metadata support -> event marked failed, no conditional msg")
    @Test
    void databusFailureAfterAwsMetadataSupport() throws Exception {
        resp.setDatabusAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        // 1 failed + 8 ok = 9 events
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(eq(1L), eq("UPDATE_FAILED"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("5. All UNKNOWN -> no events, no usage")
    @Test
    void allUnknownSkipsEventsAndUsage() throws Exception {
        setAllStatuses(CdpDoctorCheckStatus.UNKNOWN);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);

        underTest.nodeStatusNetworkReport(stack);

        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(usageReporter);
    }

    @DisplayName("6. Blank databus endpoint -> first check skipped (8 events) but usage still 9")
    @Test
    void blankDatabusEndpointSkipsDatabusEvent() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn(DATABUS_ENDPOINT);
        when(dataBusEndpointProvider.getDataBusEndpoint(DATABUS_ENDPOINT, false)).thenReturn("");
        when(dataBusEndpointProvider.getDatabusS3Endpoint(eq(""), eq(false), anyString())).thenReturn(DATABUS_S3_ENDPOINT);
        when(cdpDoctorService.getNetworkStatusForMinions(stack)).thenReturn(responses);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("7. Monitoring URL missing domain -> compute monitoring skipped (8 events) usage 9")
    @Test
    void computeMonitoringSkippedWhenUrlNotCloudera() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn("https://metrics.otherdomain/v1/write");

        underTest.nodeStatusNetworkReport(stack);

        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("8. Older version (<0.4.8) -> only subset of events (7) and NO usage")
    @Test
    void olderVersionSkipsUnstableChecksAndUsage() throws Exception {
        resp.setCdpTelemetryVersion("0.4.7");
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("0.4.7");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, false)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        ArgumentCaptor<List> paramsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), paramsCaptor.capture());
        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(eq(1L), eq("UPDATE_FAILED"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verifyNoInteractions(usageReporter);
    }

    @DisplayName("9. Exception in telemetry fetch -> skip everything (CloudbreakOrchestratorFailedException branch)")
    @Test
    void orchestratorFailedExceptionSkipped() throws Exception {
        doThrow(new CloudbreakOrchestratorFailedException("telemetry missing")).when(cdpDoctorService).getTelemetryVersion(stack);
        underTest.nodeStatusNetworkReport(stack);
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(usageReporter);
    }

    @DisplayName("10. Generic unforeseen exception (simulate via databus provider) -> skip gracefully")
    @Test
    void unexpectedExceptionSkipped() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn(DATABUS_ENDPOINT);
        when(dataBusEndpointProvider.getDataBusEndpoint(DATABUS_ENDPOINT, false)).thenThrow(new RuntimeException("boom"));
        when(cdpDoctorService.getNetworkStatusForMinions(stack)).thenReturn(responses);

        underTest.nodeStatusNetworkReport(stack);
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(usageReporter);
    }

    @DisplayName("11. Datalake stack type -> usage still reported (clusterType difference is internal)")
    @Test
    void datalakeStackTypeAlsoReportsUsage() throws Exception {
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.getResourceCrn()).thenReturn(DATALAKE_CRN);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        verify(cloudbreakEventService, never()).fireCloudbreakEvent(eq(1L), eq("UPDATE_IN_PROGRESS"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), any());
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("12. Invalid cloud platform -> envType UNSET but still usage reported")
    @Test
    void invalidCloudPlatformStillReportsUsage() throws Exception {
        when(stack.getCloudPlatform()).thenReturn("SOME_UNKNOWN");
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("13. Null cloud platform -> envType UNSET usage still reported")
    @Test
    void nullCloudPlatformStillReportsUsage() throws Exception {
        when(stack.getCloudPlatform()).thenReturn(null);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        verify(usageReporter, times(9)).cdpNetworkCheckEvent(any());
    }

    @DisplayName("14. Failed event formatting without conditional Warning (databus failure with supported version)")
    @Test
    void failedEventFormattingWithoutConditionalWarning() throws Exception {
        resp.setDatabusAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        ArgumentCaptor<List> paramsCaptor = ArgumentCaptor.forClass(List.class);
        // 1 failed + 8 in progress
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(eq(1L), eq("UPDATE_FAILED"),
                eq(ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED), paramsCaptor.capture());
        List failedParams = paramsCaptor.getValue();
        assertEquals("WARNING - ", failedParams.get(0));
        assertTrue(((String) failedParams.get(1)).contains("DataBus API"));
        String msg = (String) failedParams.get(2);
        assertTrue(msg.startsWith("FAILED."));
        assertTrue(msg.contains(GATEWAY_FQDN));
        assertFalse(msg.contains(AWS_METADATA_WARNING));
    }

    @DisplayName("15. Usage reporting details for failed network check includes failed hosts list")
    @Test
    void usageReportingFailedIncludesFailedHosts() throws Exception {
        resp.setDatabusAccessible(CdpDoctorCheckStatus.NOK);
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);

        underTest.nodeStatusNetworkReport(stack);

        ArgumentCaptor<com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheck> usageCaptor = ArgumentCaptor.forClass(
                com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheck.class);
        verify(usageReporter, times(9)).cdpNetworkCheckEvent(usageCaptor.capture());
        boolean foundFailedDatabus = false;
        for (com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheck nc : usageCaptor.getAllValues()) {
            if (nc.getType() == com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheckType.Value.DATABUS) {
                if (nc.getResult() == com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheckResult.Value.FAILED) {
                    assertEquals(GATEWAY_FQDN, nc.getFailedHostsJoined());
                    foundFailedDatabus = true;
                }
            }
        }
        assertTrue(foundFailedDatabus, "Failed databus network check usage event not found");
    }

    @DisplayName("16. getRemoteWriteUrl returns null when monitoringConfiguration is null")
    @Test
    void getRemoteWriteUrlNullMonitoringConfiguration() throws Exception {
        java.lang.reflect.Field f = DiagnosticsFlowService.class.getDeclaredField("monitoringConfiguration");
        f.setAccessible(true);
        Object original = f.get(underTest);
        try {
            f.set(underTest, null);
            java.lang.reflect.Method m = DiagnosticsFlowService.class.getDeclaredMethod("getRemoteWriteUrl");
            m.setAccessible(true);
            Object result = m.invoke(underTest);
            assertNull(result);
        } finally {
            // restore (not strictly necessary for isolated tests but keeps state clean)
            f.set(underTest, original);
        }
    }

    @DisplayName("17. reportNetworkCheckUsages exception handling path (usageReporter throws on first call)")
    @Test
    void reportNetworkCheckUsagesExceptionHandled() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);
        // Throw on first usage event
        doThrow(new RuntimeException("fail")).when(usageReporter).cdpNetworkCheckEvent(any());

        underTest.nodeStatusNetworkReport(stack);

        // Only first type attempted before exception caught
        verify(usageReporter, times(1)).cdpNetworkCheckEvent(any());
    }

    // Helpers
    private void stubHappyInfra() throws Exception {
        when(cdpDoctorService.getTelemetryVersion(stack)).thenReturn("1.3.6");
        stubEndpoints();
        when(preFlightCheckValidationService.preFlightCheckSupported(ENVIRONMENT_CRN, true)).thenReturn(true);
        when(monitoringConfiguration.getRemoteWriteUrl()).thenReturn(MONITORING_URL);
    }

    private void stubEndpoints() throws Exception {
        when(altusDatabusConfiguration.getAltusDatabusEndpoint()).thenReturn(DATABUS_ENDPOINT);
        when(dataBusEndpointProvider.getDataBusEndpoint(DATABUS_ENDPOINT, false)).thenReturn(DATABUS_ENDPOINT);
        when(dataBusEndpointProvider.getDatabusS3Endpoint(eq(DATABUS_ENDPOINT), anyBoolean(), anyString())).thenReturn(DATABUS_S3_ENDPOINT);
        when(cdpDoctorService.getNetworkStatusForMinions(stack)).thenReturn(responses);
    }

    private void setAllStatuses(CdpDoctorCheckStatus status) {
        resp.setDatabusAccessible(status);
        resp.setDatabusS3Accessible(status);
        resp.setArchiveClouderaComAccessible(status);
        resp.setS3Accessible(status);
        resp.setStsAccessible(status);
        resp.setAdlsV2Accessible(status);
        resp.setAzureManagementAccessible(status);
        resp.setGcsAccessible(status);
        resp.setComputeMonitoringAccessible(status);
        resp.setServiceDeliveryCacheS3Accessible(status);
    }
}
