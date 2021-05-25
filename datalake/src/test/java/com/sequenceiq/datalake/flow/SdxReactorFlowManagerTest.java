package com.sequenceiq.datalake.flow;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.bus.Event;
import reactor.bus.EventBus;

import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_EVENT;
import static com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT;

@RunWith(MockitoJUnitRunner.class)
public class SdxReactorFlowManagerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final String LOG_LOCATION = "s3a://path/to/logs";

    private static final String IMAGE_ID = "image-id-first";

    private static final String CLOUD_PLATFORM = "Azure";

    private static final SdxUpgradeReplaceVms REPAIR_AFTER_UPGRADE = SdxUpgradeReplaceVms.ENABLED;

    @Mock
    private EntitlementService entitlementService;

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

    @Before
    public void setUp() {
        sdxCluster = getValidSdxCluster();
        BaseFlowEvent baseFlowEvent = new BaseFlowEvent("dontcare", 1L, "crn");
        when(eventFactory.createEventWithErrHandler(anyMap(), any(Acceptable.class)))
                .thenReturn(new Event<Acceptable>(baseFlowEvent));
    }

    @Test
    public void testSdxBackupOnUpgradeRequestEnabled() {
        when(environmentClientService.getBackupLocation(ENV_CRN)).thenReturn(BACKUP_LOCATION);
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(true);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        try {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                    underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED));
        } catch (Exception e) {
        }
        verify(reactor, times(1)).notify(eq(DATALAKE_UPGRADE_FLOW_CHAIN_EVENT.event()), any(Event.class));
    }

    @Test (expected = BadRequestException.class)
    public void testSdxBackupOnUpgradeRequestEnabledFailure() {
        when(environmentClientService.getBackupLocation(ENV_CRN)).thenThrow(new BadRequestException("No backup location"));
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(true);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED));
    }

    @Test
    public void testSdxBackupOnUpgradeRequestDisabled() {
        when(entitlementService.isDatalakeBackupOnUpgradeEnabled(any())).thenReturn(false);
        try {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                    underTest.triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.DISABLED));
        } catch (Exception e) {
        }
        verify(reactor, times(1)).notify(eq(DATALAKE_UPGRADE_EVENT.event()), any(Event.class));
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
}
