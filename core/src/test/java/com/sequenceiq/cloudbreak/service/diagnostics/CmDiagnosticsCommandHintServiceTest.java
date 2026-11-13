package com.sequenceiq.cloudbreak.service.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class CmDiagnosticsCommandHintServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:1234:cluster:2";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String ACCOUNT_ID = "1234";

    private static final String FLOW_CHAIN_ID = "flow-chain-id";

    private static final Instant NOW = Instant.parse("2026-11-13T14:05:00Z");

    private static final long SHORT_CHAIN_START_MILLIS = Instant.parse("2026-11-13T13:00:30Z").toEpochMilli();

    private static final long LONG_CHAIN_START_MILLIS = Instant.parse("2026-11-12T10:00:00Z").toEpochMilli();

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private Clock clock;

    @Mock
    private StackView stack;

    @InjectMocks
    private CmDiagnosticsCommandHintService underTest;

    @Test
    void firesDatahubCommandWithShortChainStart() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(s3Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        // short chain: chainStart (13:00:30Z) minus 15 min margin
        assertThat(firedCommand()).isEqualTo("cdp datahub collect-cm-diagnostics --stack-crn " + DATAHUB_CRN
                + " --destination CLOUD_STORAGE --start-time 2026-11-13T12:45:30Z --end-time 2026-11-13T14:05:00Z");
    }

    @Test
    void capsLongChainToMaxLookback() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(s3Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(LONG_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        // long chain: start is capped to now minus 6 hours
        assertThat(firedCommand()).contains(" --start-time 2026-11-13T08:05:00Z --end-time 2026-11-13T14:05:00Z");
    }

    @Test
    void firesDatalakeCommand() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATALAKE_CRN);
        given(stack.isDatahub()).willReturn(false);
        given(stack.isDatalake()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled("acc1")).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(s3Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        assertThat(firedCommand()).startsWith("cdp datalake collect-cm-diagnostics --stack-crn " + DATALAKE_CRN);
    }

    @Test
    void includesRolesWhenGiven() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(s3Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of("NAMENODE", "DATANODE"));

        assertThat(firedCommand()).endsWith(" --roles NAMENODE DATANODE");
    }

    @Test
    void doesNotFireWhenEntitlementDisabled() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(false);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        verify(flowMessageService, never()).fireEventAndLog(any(), anyString(), any(), any());
    }

    @Test
    void doesNotFireForUnsupportedStackType() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(false);
        given(stack.isDatalake()).willReturn(false);
        given(stack.getType()).willReturn(StackType.TEMPLATE);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        verify(flowMessageService, never()).fireEventAndLog(any(), anyString(), any(), any());
    }

    @Test
    void doesNotFireWhenTelemetryMissing() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(null);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        verify(flowMessageService, never()).fireEventAndLog(any(), anyString(), any(), any());
    }

    @Test
    void firesSupportCommandWhenLoggingMissing() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(null));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        assertThat(firedCommand()).isEqualTo("cdp datahub collect-cm-diagnostics --stack-crn " + DATAHUB_CRN
                + " --destination SUPPORT --ticket <support-case-number> --start-time 2026-11-13T12:45:30Z --end-time 2026-11-13T14:05:00Z");
    }

    @Test
    void firesSupportCommandWhenNoCloudStorageLogging() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(new Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        assertThat(firedCommand()).contains(" --destination SUPPORT --ticket <support-case-number> ");
    }

    @ParameterizedTest
    @MethodSource("cloudStorageLoggings")
    void firesCloudStorageDestinationForEachCloudStorageType(Logging logging) {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(logging));
        given(clock.getCurrentInstant()).willReturn(NOW);
        givenChainStart(SHORT_CHAIN_START_MILLIS);

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        assertThat(firedCommand()).contains(" --destination CLOUD_STORAGE ").doesNotContain("--ticket");
    }

    @Test
    void fallsBackWhenFlowStartUnknown() {
        given(stackDtoService.getStackViewById(STACK_ID)).willReturn(stack);
        given(stack.getResourceCrn()).willReturn(DATAHUB_CRN);
        given(stack.isDatahub()).willReturn(true);
        given(entitlementService.isDiagnosticsEnabled(ACCOUNT_ID)).willReturn(true);
        given(componentConfigProviderService.getTelemetry(STACK_ID)).willReturn(telemetryWith(s3Logging()));
        given(clock.getCurrentInstant()).willReturn(NOW);
        given(flowLogDBService.getLastFlowLog(STACK_ID)).willReturn(Optional.empty());

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        // fallback is now minus 6 hours
        assertThat(firedCommand()).contains(" --start-time 2026-11-13T08:05:00Z --end-time 2026-11-13T14:05:00Z");
    }

    @Test
    void swallowsExceptionFromStackLookup() {
        given(stackDtoService.getStackViewById(STACK_ID)).willThrow(new RuntimeException("boom"));

        underTest.fireCmDiagnosticsHint(STACK_ID, List.of());

        verify(flowMessageService, never()).fireEventAndLog(any(), anyString(), any(), any());
    }

    private static Stream<Arguments> cloudStorageLoggings() {
        return Stream.of(
                Arguments.of(abfsLogging()),
                Arguments.of(gcsLogging()),
                Arguments.of(s3Logging()));
    }

    private static Telemetry telemetryWith(Logging logging) {
        Telemetry telemetry = new Telemetry();
        telemetry.setLogging(logging);
        return telemetry;
    }

    private static Logging s3Logging() {
        Logging logging = new Logging();
        logging.setS3(new S3CloudStorageV1Parameters());
        return logging;
    }

    private static Logging abfsLogging() {
        Logging logging = new Logging();
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        return logging;
    }

    private static Logging gcsLogging() {
        Logging logging = new Logging();
        logging.setGcs(new GcsCloudStorageV1Parameters());
        return logging;
    }

    private void givenChainStart(long createdMillis) {
        FlowLog lastFlowLog = new FlowLog();
        lastFlowLog.setFlowChainId(FLOW_CHAIN_ID);
        FlowLogWithoutPayload chainLog = mock(FlowLogWithoutPayload.class);
        given(chainLog.getCreated()).willReturn(createdMillis);
        given(flowLogDBService.getLastFlowLog(STACK_ID)).willReturn(Optional.of(lastFlowLog));
        given(flowLogDBService.getFlowLogsWithoutPayloadByFlowChainIdsCreatedDesc(Set.of(FLOW_CHAIN_ID)))
                .willReturn(List.of(chainLog));
    }

    private String firedCommand() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()),
                eq(ResourceEvent.CLUSTER_CM_DIAGNOSTICS_COLLECTION_HINT), captor.capture());
        return captor.getValue();
    }
}
