package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent.SDX_RESIZE_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmStateSelectors.UPGRADE_CCM_UPGRADE_STACK_EVENT;
import static com.sequenceiq.flow.api.model.FlowType.FLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.dr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;

@ExtendWith(MockitoExtension.class)
class SdxReactorFlowManagerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String IMAGE_ID = "image-id-first";

    private static final boolean SKIP_BACKUP = false;

    private static final boolean ROLLING_UPGRADE_ENABLED = true;

    private static final DatalakeDrSkipOptions SKIP_OPTIONS = new DatalakeDrSkipOptions(false, false, false);

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private DatalakeDrConfig datalakeDrConfig;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private EventBus reactor;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @InjectMocks
    private SdxReactorFlowManager underTest;

    private SdxCluster sdxCluster;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private Promise<AcceptResult> acceptResultPromise;

    @BeforeEach
    void setUp() throws InterruptedException {
        sdxCluster = getValidSdxCluster();
        when(acceptResultPromise.await(anyLong(), any())).thenReturn(FlowAcceptResult.runningInFlow("flowId"));
        BaseFlowEvent baseFlowEvent = new BaseFlowEvent("dontcare", 1L, "crn", acceptResultPromise);
        lenient().when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(new Event(baseFlowEvent));
    }

    @Test
    void testSdxBackupOnUpgradeSupportedPlatform() {
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(true);
        when(sdxBackupRestoreService.shouldSdxBackupBePerformed(any(), eq(true))).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, SKIP_BACKUP,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(1)).notify(eq(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT), any(Event.class));
    }

    @Test
    void testSdxBackupOnUpgradeUnSupportedRuntimes() {
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(true);
        when(sdxBackupRestoreService.shouldSdxBackupBePerformed(any(), eq(true))).thenReturn(false);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, SKIP_BACKUP,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(0)).notify(eq(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT), any(Event.class));

        when(sdxBackupRestoreService.shouldSdxBackupBePerformed(any(), eq(true))).thenReturn(false);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, SKIP_BACKUP,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(0)).notify(eq(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT), any(Event.class));
    }

    @Test
    void testSdxBackupOnUpgradeRazEnabled() {
        sdxCluster = getValidSdxCluster("7.2.10");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, SKIP_BACKUP,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(0)).notify(eq(DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT), any(Event.class));
    }

    @Test
    void testSdxBackupOnUpgradeRequestDisabled() {
        sdxCluster = getValidSdxCluster("7.2.10");
        sdxCluster.setRangerRazEnabled(false);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(false);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, SKIP_BACKUP,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(1)).notify(eq(DATALAKE_UPGRADE_EVENT.event()), any(Event.class));
    }

    @Test
    void testSdxUpgradeRequestBackupNotRequested() {
        sdxCluster = getValidSdxCluster("7.2.10");
        sdxCluster.setRangerRazEnabled(false);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED, true,
                        SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED));
        verify(reactor, times(1)).notify(eq(DATALAKE_UPGRADE_EVENT.event()), any(Event.class));
    }

    @Test
    void testTriggerCcmUpgradeFlow() {
        sdxCluster = getValidSdxCluster("7.2.10");
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerCcmUpgradeFlow(sdxCluster));
        verify(reactor, times(1)).notify(eq(UPGRADE_CCM_UPGRADE_STACK_EVENT.event()), any(Event.class));
    }

    @Test
    public void testTriggerDatalakeRuntimeUpgradePreparationFlow() {
        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerDatalakeRuntimeUpgradePreparationFlow(sdxCluster, IMAGE_ID,
                false));
        verify(reactor, times(1)).notify(eq(DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT.event()), any(Event.class));
        assertEquals(FLOW, result.getType());
        assertEquals("flowId", result.getPollableId());
    }

    @Test
    void testTriggerSdxResizeEventSend() {
        SdxCluster sdxCluster = getValidSdxCluster();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerSdxResize(sdxCluster.getId(), sdxCluster, SKIP_OPTIONS));
        verify(eventSenderService, times(1)).sendEventAndNotification(eq(sdxCluster), eq(ResourceEvent.DATALAKE_RESIZE_TRIGGERED));
        verify(reactor, times(1)).notify(eq(SDX_RESIZE_FLOW_CHAIN_START_EVENT), any(Event.class));
    }

    @Test
    void testTriggerSaltUpdate() {
        SdxCluster sdxCluster = getValidSdxCluster();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerSaltUpdate(sdxCluster));
        verify(reactor, times(1)).notify(eq(SALT_UPDATE_EVENT.event()), any(Event.class));
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setEnvCrn(ENV_CRN);
        return sdxCluster;
    }

    private SdxCluster getValidSdxCluster(String runtime) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setEnvCrn(ENV_CRN);
        sdxCluster.setRuntime(runtime);
        return sdxCluster;
    }

}
