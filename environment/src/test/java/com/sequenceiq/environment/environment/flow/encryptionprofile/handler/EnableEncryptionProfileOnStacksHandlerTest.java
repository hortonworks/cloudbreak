package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileClusterResolver;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@ExtendWith(MockitoExtension.class)
class EnableEncryptionProfileOnStacksHandlerTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:e1";

    private static final String ENV_NAME = "envName";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:1234:encryptionProfile:ep";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:dl";

    private static final String DATAHUB_CRN_1 = "crn:cdp:datahub:us-west-1:1234:cluster:dh1";

    private static final String DATAHUB_CRN_2 = "crn:cdp:datahub:us-west-1:1234:cluster:dh2";

    private static final FlowIdentifier DATALAKE_FLOW = new FlowIdentifier(FlowType.FLOW, "dl-flow");

    private static final FlowIdentifier DATAHUB_FLOW_1 = new FlowIdentifier(FlowType.FLOW, "dh-flow-1");

    private static final FlowIdentifier DATAHUB_FLOW_2 = new FlowIdentifier(FlowType.FLOW_CHAIN, "dh-flow-2");

    private static final String SKIPPED_DATAHUBS = "Data Hub 'dh1' (skipped because the Data Lake update failed), "
            + "Data Hub 'dh2' (skipped because the Data Lake update failed)";

    @Mock
    private StackService stackService;

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

    @Mock
    private EncryptionProfileClusterResolver encryptionProfileClusterResolver;

    @Mock
    private SdxPollerProvider sdxPollerProvider;

    @Mock
    private DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator;

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator;

    @InjectMocks
    private EnableEncryptionProfileOnStacksHandler underTest;

    private EnableEncryptionProfileEvent event;

    private StackViewV4Response datalake;

    private StackViewV4Response datahub1;

    private StackViewV4Response datahub2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxTime", 1);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
        event = new EnableEncryptionProfileEvent(ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name(),
                ENV_ID, ENV_NAME, ENV_CRN, ENCRYPTION_PROFILE_CRN);
        datalake = stack("dl", DATALAKE_CRN, true);
        datahub1 = stack("dh1", DATAHUB_CRN_1, false);
        datahub2 = stack("dh2", DATAHUB_CRN_2, false);
        EnvironmentInMemoryStateStore.put(ENV_ID, PollGroup.POLLABLE);
    }

    @AfterEach
    void tearDown() {
        EnvironmentInMemoryStateStore.delete(ENV_ID);
    }

    @Test
    void testSelector() {
        assertThat(underTest.selector()).isEqualTo(ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(ENV_ID, new Exception("failed"), new Event<>(event));
        assertThat(response.getSelector()).isEqualTo(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(response.getResourceId()).isEqualTo(ENV_ID);
        assertThat(response).isInstanceOf(EnableEncryptionProfileFailedEvent.class);
    }

    @Test
    void testDoAcceptTriggersDatalakeAndDatahubs() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(DATALAKE_FLOW);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenReturn(DATAHUB_FLOW_1);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_2, null)).thenReturn(DATAHUB_FLOW_2);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW))).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(List.of(DATALAKE_FLOW))).thenReturn(false);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService).enableEncryptionProfile(DATALAKE_CRN, null);
        verify(datahubService).enableEncryptionProfile(DATAHUB_CRN_1, null);
        verify(datahubService).enableEncryptionProfile(DATAHUB_CRN_2, null);
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(response.getResourceId()).isEqualTo(ENV_ID);
        assertThat(((EnableEncryptionProfileEvent) response).getEncryptionProfileCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testDoAcceptWithoutClusters() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(datahubService, never()).enableEncryptionProfile(any(), any());
        verify(sdxPollerProvider, never()).flowListPoller(any(), any());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsClustersWithOwnEncryptionProfile() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datalake)).thenReturn(true);
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub1)).thenReturn(false);
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub2)).thenReturn(true);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenReturn(DATAHUB_FLOW_1);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_1))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_1))).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(datahubService, never()).enableEncryptionProfile(eq(DATAHUB_CRN_2), any());
        verify(datahubService).enableEncryptionProfile(DATAHUB_CRN_1, null);
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDatalakeTriggerFailureSkipsDatahubsAndReportsThem() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenThrow(new SdxOperationFailedException("dl is busy"));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).enableEncryptionProfile(any(), any());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
        assertFailedWith(response, "Data Lake 'dl' (dl is busy), " + SKIPPED_DATAHUBS);
    }

    @Test
    void testDatalakeFlowFailureSkipsDatahubsAndReportsThem() {
        SdxClusterDetailResponse detail = new SdxClusterDetailResponse();
        detail.setStatusReason("CM restart failed");
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(DATALAKE_FLOW);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW))).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(List.of(DATALAKE_FLOW))).thenReturn(true);
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(detail);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).enableEncryptionProfile(any(), any());
        assertFailedWith(response, "Data Lake 'dl' (CM restart failed), " + SKIPPED_DATAHUBS);
    }

    @Test
    void testDatalakeEncryptionProfileLookupFailureSkipsDatahubs() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datalake)).thenThrow(new SdxOperationFailedException("dl not found"));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(datahubService, never()).enableEncryptionProfile(any(), any());
        assertFailedWith(response, "Data Lake 'dl' (dl not found), " + SKIPPED_DATAHUBS);
    }

    @Test
    void testDatalakeFailureReportsDatahubLookupFailuresAndSkipsTheOthers() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datalake)).thenReturn(false);
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub1)).thenThrow(new DatahubOperationFailedException("dh1 not found"));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub2)).thenReturn(false);
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenThrow(new SdxOperationFailedException("dl is busy"));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).enableEncryptionProfile(any(), any());
        assertFailedWith(response, "Data Lake 'dl' (dl is busy), Data Hub 'dh1' (dh1 not found), "
                + "Data Hub 'dh2' (skipped because the Data Lake update failed)");
    }

    @Test
    void testDatalakePollingTimeoutReportsDatalakeName() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(DATALAKE_FLOW);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW))).thenReturn(AttemptResults::justContinue);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datalakeMultipleFlowsResultEvaluator, never()).anyFailed(anyList());
        assertFailedWith(response, "Data Lake 'dl' (polling timed out after 1 seconds)");
    }

    @Test
    void testDatalakePollingCancelledStopsTheHandler() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datalake, datahub1));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(DATALAKE_FLOW);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW))).thenReturn(() -> {
            EnvironmentInMemoryStateStore.put(ENV_ID, PollGroup.CANCELLED);
            throw new PollerStoppedException("Sdx polling cancelled");
        });

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(PollerStoppedException.class);
        verify(datahubService, never()).enableEncryptionProfile(any(), any());
    }

    @Test
    void testDatahubPollingCancelledStopsTheHandler() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datahub1));
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenReturn(DATAHUB_FLOW_1);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_1))).thenReturn(() -> {
            EnvironmentInMemoryStateStore.put(ENV_ID, PollGroup.CANCELLED);
            throw new PollerStoppedException("Datahub flow polling cancelled");
        });

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(PollerStoppedException.class);
        verify(datahubMultipleFlowsResultEvaluator, never()).collectFailed(anyList());
    }

    @Test
    void testDatahubTriggerAndFlowFailuresAreAllReported() {
        StackV4Response dh2Detail = new StackV4Response();
        dh2Detail.setStatusReason("salt failed");
        StackViewV4Response datahub3 = stack("dh3", "crn:cdp:datahub:us-west-1:1234:cluster:dh3", false);
        FlowIdentifier datahubFlow3 = new FlowIdentifier(FlowType.FLOW, "dh-flow-3");
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datahub1, datahub2, datahub3));
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenThrow(new DatahubOperationFailedException("trigger failed"));
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_2, null)).thenReturn(DATAHUB_FLOW_2);
        when(datahubService.enableEncryptionProfile(datahub3.getCrn(), null)).thenReturn(datahubFlow3);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_2, datahubFlow3))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_2, datahubFlow3))).thenReturn(List.of(DATAHUB_FLOW_2));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_2), anySet())).thenReturn(dh2Detail);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertFailedWith(response, "Data Hub 'dh1' (trigger failed), Data Hub 'dh2' (salt failed)");
    }

    @Test
    void testDatahubPollingTimeoutReportsNotFinishedDatahubs() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datahub1, datahub2));
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenReturn(DATAHUB_FLOW_1);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_2, null)).thenReturn(DATAHUB_FLOW_2);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(AttemptResults::justContinue);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(List.of(DATAHUB_FLOW_2));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertFailedWith(response, "Data Hub 'dh2' (polling timed out after 1 seconds)");
    }

    @Test
    void testDatahubPollingTimeoutReportsDatahubStatusReasonAndPollingError() {
        StackV4Response dh2Detail = new StackV4Response();
        dh2Detail.setStatusReason("salt failed");
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datahub1, datahub2));
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_1, null)).thenReturn(DATAHUB_FLOW_1);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_2, null)).thenReturn(DATAHUB_FLOW_2);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(AttemptResults::justContinue);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_1, DATAHUB_FLOW_2))).thenReturn(List.of(DATAHUB_FLOW_2));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_2), anySet())).thenReturn(dh2Detail);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertFailedWith(response, "Data Hub 'dh2' (salt failed; polling timed out after 1 seconds)");
    }

    @Test
    void testEncryptionProfileLookupFailureIsReportedAndOtherClustersAreUpdated() {
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(datahub1, datahub2));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub1)).thenThrow(new DatahubOperationFailedException("not found"));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(datahub2)).thenReturn(false);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN_2, null)).thenReturn(DATAHUB_FLOW_2);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW_2))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW_2))).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).enableEncryptionProfile(eq(DATAHUB_CRN_1), any());
        verify(datahubService).enableEncryptionProfile(DATAHUB_CRN_2, null);
        assertFailedWith(response, "Data Hub 'dh1' (not found)");
    }

    private void assertFailedWith(Selectable response, String failedClusters) {
        assertThat(response).isInstanceOf(EnableEncryptionProfileFailedEvent.class);
        EnableEncryptionProfileFailedEvent failedEvent = (EnableEncryptionProfileFailedEvent) response;
        assertThat(failedEvent.getSelector()).isEqualTo(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(failedEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(failedEvent.getResourceCrn()).isEqualTo(ENV_CRN);
        assertThat(failedEvent.getException()).isInstanceOf(CloudbreakServiceException.class);
        assertThat(failedEvent.getException().getMessage()).isEqualTo("Failed to enable encryption profile on the following cluster(s): "
                + failedClusters + ". The environment encryption profile was set; retry the operation or run disable encryption profile.");
    }

    private StackViewV4Response stack(String name, String crn, boolean datalakeStack) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setName(name);
        stack.setCrn(crn);
        lenient().when(encryptionProfileClusterResolver.isDatalake(stack)).thenReturn(datalakeStack);
        return stack;
    }
}
