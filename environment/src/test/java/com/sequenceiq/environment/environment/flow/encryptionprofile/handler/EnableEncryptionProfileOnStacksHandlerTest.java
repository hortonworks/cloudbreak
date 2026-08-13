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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ExtendWith(MockitoExtension.class)
class EnableEncryptionProfileOnStacksHandlerTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:e1";

    private static final String ENV_NAME = "envName";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:1234:encryptionProfile:ep";

    private static final String LEGACY_ENCRYPTION_PROFILE_CRN =
            "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_v1";

    private static final String NON_LEGACY_DEFAULT_ENCRYPTION_PROFILE_CRN =
            "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_140_3";

    private static final String OWN_ENCRYPTION_PROFILE_CRN =
            "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-user-defined-profile";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:dl";

    private static final String DATAHUB_CRN_1 = "crn:cdp:datahub:us-west-1:1234:cluster:dh1";

    private static final String DATAHUB_CRN_2 = "crn:cdp:datahub:us-west-1:1234:cluster:dh2";

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxTime", 1);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
        event = new EnableEncryptionProfileEvent(ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name(),
                ENV_ID, ENV_NAME, ENV_CRN, ENCRYPTION_PROFILE_CRN);
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
        FlowIdentifier datalakeFlow = new FlowIdentifier(FlowType.FLOW, "dl-flow");
        FlowIdentifier datahubFlow1 = new FlowIdentifier(FlowType.FLOW, "dh-flow-1");
        FlowIdentifier datahubFlow2 = new FlowIdentifier(FlowType.FLOW, "dh-flow-2");
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(null));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(datalakeFlow);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(
                availableDatahub(DATAHUB_CRN_1), availableDatahub(DATAHUB_CRN_2))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_2), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null)).thenReturn(datahubFlow1);
        when(datahubService.updateSslConfigs(DATAHUB_CRN_2, null)).thenReturn(datahubFlow2);
        when(sdxPollerProvider.flowListPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(anyList())).thenReturn(false);
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, times(1)).enableEncryptionProfile(DATALAKE_CRN, null);
        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_1, null);
        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_2, null);
        verify(sdxPollerProvider, times(1)).flowListPoller(eq(ENV_ID), anyList());
        verify(datahubPollerProvider, times(1)).multipleFlowsPoller(eq(ENV_ID), anyList());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(response.getResourceId()).isEqualTo(ENV_ID);
    }

    @Test
    void testDoAcceptSkipsDatalakeWhenNoRunningSdx() {
        SdxClusterResponse stoppedSdx = new SdxClusterResponse();
        stoppedSdx.setCrn(DATALAKE_CRN);
        stoppedSdx.setStatus(SdxClusterStatusResponse.STOPPED);
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(stoppedSdx));
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(availableDatahub(DATAHUB_CRN_1))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "dh-flow-1"));
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(sdxService, never()).getDetailByCrn(any());
        verify(sdxPollerProvider, never()).flowListPoller(any(), any());
        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_1, null);
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsDatahubsWhenNullResponses() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(null));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).updateSslConfigs(any(), any());
        verify(datahubService, never()).getByCrn(any(), anySet());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsDatahubsWhenNoneAvailable() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        StackViewV4Response stoppedDatahub = new StackViewV4Response();
        stoppedDatahub.setCrn(DATAHUB_CRN_1);
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(Status.STOPPED);
        stoppedDatahub.setCluster(cluster);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(stoppedDatahub)));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).updateSslConfigs(any(), any());
        verify(datahubService, never()).getByCrn(any(), anySet());
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsDatalakeWithOwnEncryptionProfile() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(OWN_ENCRYPTION_PROFILE_CRN));
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of()));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(sdxPollerProvider, never()).flowListPoller(any(), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsDatahubsWithOwnEncryptionProfile() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(
                availableDatahub(DATAHUB_CRN_1), availableDatahub(DATAHUB_CRN_2))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_2), anySet())).thenReturn(datahubDetail(OWN_ENCRYPTION_PROFILE_CRN));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "dh-flow-1"));
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_1, null);
        verify(datahubService, never()).updateSslConfigs(eq(DATAHUB_CRN_2), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptTreatsLegacyProfileAsNoOwnProfile() {
        FlowIdentifier datalakeFlow = new FlowIdentifier(FlowType.FLOW, "dl-flow");
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(LEGACY_ENCRYPTION_PROFILE_CRN));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(datalakeFlow);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(availableDatahub(DATAHUB_CRN_1))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(LEGACY_ENCRYPTION_PROFILE_CRN));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "dh-flow-1"));
        when(sdxPollerProvider.flowListPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(anyList())).thenReturn(false);
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, times(1)).enableEncryptionProfile(DATALAKE_CRN, null);
        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_1, null);
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptSkipsClustersWithNonLegacyDefaultProfile() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(NON_LEGACY_DEFAULT_ENCRYPTION_PROFILE_CRN));
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(availableDatahub(DATAHUB_CRN_1))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(NON_LEGACY_DEFAULT_ENCRYPTION_PROFILE_CRN));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(sdxService, never()).enableEncryptionProfile(any(), any());
        verify(datahubService, never()).updateSslConfigs(any(), any());
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    @Test
    void testDoAcceptWhenDatalakeFlowFailsPropagatesToFramework() {
        FlowIdentifier datalakeFlow = new FlowIdentifier(FlowType.FLOW, "dl-flow");
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(null));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(datalakeFlow);
        when(sdxPollerProvider.flowListPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(anyList())).thenReturn(true);

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(SdxOperationFailedException.class);
        verify(datahubService, never()).updateSslConfigs(any(), any());
    }

    @Test
    void testDoAcceptWhenDatahubFlowsFailPropagatesTheException() {
        FlowIdentifier datahubFlow = new FlowIdentifier(FlowType.FLOW, "dh-flow-1");
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(availableDatahub(DATAHUB_CRN_1))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null)).thenReturn(datahubFlow);
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of(datahubFlow));

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(DatahubOperationFailedException.class);
    }

    @Test
    void testDoAcceptWhenDatalakePollerStopped() {
        FlowIdentifier datalakeFlow = new FlowIdentifier(FlowType.FLOW, "dl-flow");
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of(runningSdx(DATALAKE_CRN)));
        when(sdxService.getDetailByCrn(DATALAKE_CRN)).thenReturn(datalakeDetail(null));
        when(sdxService.enableEncryptionProfile(DATALAKE_CRN, null)).thenReturn(datalakeFlow);
        when(sdxPollerProvider.flowListPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justContinue);

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(CloudbreakServiceException.class);
        verify(datahubService, never()).updateSslConfigs(any(), any());
    }

    @Test
    void testDoAcceptWhenDatahubPollerStopped() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(availableDatahub(DATAHUB_CRN_1))));
        lenient().when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_1, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "dh-flow-1"));
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justContinue);

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(CloudbreakServiceException.class);
    }

    @Test
    void testDoAcceptSkipsDatahubWhenGetByCrnFails() {
        when(sdxService.listByEnvironmentCrn(ENV_CRN)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(
                availableDatahub(DATAHUB_CRN_1), availableDatahub(DATAHUB_CRN_2))));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_1), anySet())).thenThrow(new DatahubOperationFailedException("boom"));
        when(datahubService.getByCrn(eq(DATAHUB_CRN_2), anySet())).thenReturn(datahubDetail(null));
        when(datahubService.updateSslConfigs(DATAHUB_CRN_2, null))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "dh-flow-2"));
        when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), anyList())).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(anyList())).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(datahubService, never()).updateSslConfigs(eq(DATAHUB_CRN_1), any());
        verify(datahubService, times(1)).updateSslConfigs(DATAHUB_CRN_2, null);
        assertThat(response.getSelector()).isEqualTo(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector());
    }

    private SdxClusterResponse runningSdx(String crn) {
        SdxClusterResponse sdx = new SdxClusterResponse();
        sdx.setCrn(crn);
        sdx.setStatus(SdxClusterStatusResponse.RUNNING);
        return sdx;
    }

    private SdxClusterDetailResponse datalakeDetail(String encryptionProfileCrn) {
        SdxClusterDetailResponse detail = new SdxClusterDetailResponse();
        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setEncryptionProfileCrn(encryptionProfileCrn);
        stackV4Response.setCluster(cluster);
        detail.setStackV4Response(stackV4Response);
        return detail;
    }

    private StackV4Response datahubDetail(String encryptionProfileCrn) {
        StackV4Response stack = new StackV4Response();
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setEncryptionProfileCrn(encryptionProfileCrn);
        stack.setCluster(cluster);
        return stack;
    }

    private StackViewV4Response availableDatahub(String crn) {
        StackViewV4Response datahub = new StackViewV4Response();
        datahub.setCrn(crn);
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(Status.AVAILABLE);
        datahub.setCluster(cluster);
        return datahub;
    }
}
