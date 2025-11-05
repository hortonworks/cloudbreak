package com.sequenceiq.datalake.service.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StackService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;

@ExtendWith(MockitoExtension.class)
class SdxCcmUpgradeServiceTest {

    private static final String ACCOUNT_ID = "6f53f8a0-d5e8-45e6-ab11-cce9b53f7aad";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:" + UUID.randomUUID();

    private static final String CLUSTER_NAME = "SdxCluster";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private StackService stackService;

    @Mock
    private AccountIdService accountIdService;

    @InjectMocks
    private SdxCcmUpgradeService underTest;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setUp() {
        environment = new DetailedEnvironmentResponse();
        environment.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        environment.setTunnel(Tunnel.latestUpgradeTarget());
        lenient().when(environmentService.getByCrn(any())).thenReturn(environment);
    }

    @Test
    void testEnvNotLatest() {
        environment.setTunnel(Tunnel.DIRECT);
        assertThatThrownBy(() -> underTest.upgradeCcm(ENV_CRN)).isInstanceOf(BadRequestException.class);
        verify(sdxReactorFlowManager, never()).triggerCcmUpgradeFlow(any());
    }

    @Test
    void testNoDatalakeForEnv() {
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of());
        when(messagesService.getMessage(any(), any())).thenReturn("no datalake");
        SdxCcmUpgradeResponse response = underTest.upgradeCcm(ENV_CRN);
        assertThat(response.getReason()).isEqualTo("no datalake");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @Test
    void testTooManyDatalakeForEnv() {
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(getSdxCluster(), getSdxCluster()));
        assertThatThrownBy(() -> underTest.upgradeCcm(ENV_CRN)).isInstanceOf(BadRequestException.class);
        verify(sdxReactorFlowManager, never()).triggerCcmUpgradeFlow(any());
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = { "DIRECT", "CLUSTER_PROXY" }, mode = Mode.INCLUDE)
    void testNotUpgradable(Tunnel tunnel) {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(sdxCluster));
        when(stackService.getDetail(CLUSTER_NAME, null, ACCOUNT_ID)).thenReturn(getStack(tunnel, Status.AVAILABLE));
        when(messagesService.getMessage(any())).thenReturn("not upgradeable");
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCcmUpgradeResponse response = underTest.upgradeCcm(ENV_CRN);

        assertThat(response.getReason()).isEqualTo("not upgradeable");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @Test
    void testLatestSkipUpgrade() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(sdxCluster));
        when(stackService.getDetail(CLUSTER_NAME, null, ACCOUNT_ID)).thenReturn(getStack(Tunnel.CCMV2_JUMPGATE, Status.AVAILABLE));
        when(messagesService.getMessage(any())).thenReturn("latest");
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCcmUpgradeResponse response = underTest.upgradeCcm(ENV_CRN);

        assertThat(response.getReason()).isEqualTo("latest");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @Test
    void testNotAvailableError() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(sdxCluster));
        when(stackService.getDetail(CLUSTER_NAME, null, ACCOUNT_ID)).thenReturn(getStack(Tunnel.CCM, Status.STOPPED));
        when(messagesService.getMessage(any())).thenReturn("unavailable");
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCcmUpgradeResponse response = underTest.upgradeCcm(ENV_CRN);

        assertThat(response.getReason()).isEqualTo("unavailable");
        assertThat(response.getFlowIdentifier()).isEqualTo(FlowIdentifier.notTriggered());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = { "AVAILABLE", "UPGRADE_CCM_FAILED" }, mode = Mode.INCLUDE)
    void testTriggerUpgrade(Status status) {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(sdxCluster));
        when(stackService.getDetail(CLUSTER_NAME, null, ACCOUNT_ID)).thenReturn(getStack(Tunnel.CCM, Status.AVAILABLE));
        when(messagesService.getMessage(any(), any())).thenReturn("success");
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "flowId");
        when(sdxReactorFlowManager.triggerCcmUpgradeFlow(sdxCluster)).thenReturn(flowId);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCcmUpgradeResponse response = underTest.upgradeCcm(ENV_CRN);

        assertThat(response.getReason()).isEqualTo("success");
        assertThat(response.getFlowIdentifier()).isEqualTo(flowId);
    }

    @Test
    void testInitAndWaitForStackUpgrade() {
        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, "pollableId");
        StackCcmUpgradeV4Response upgradeResponse = new StackCcmUpgradeV4Response(CcmUpgradeResponseType.TRIGGERED, flowId, null, "resourceCrn");
        when(stackV4Endpoint.upgradeCcmByCrnInternal(eq(0L), eq(STACK_CRN), any())).thenReturn(upgradeResponse);
        PollingConfig pc = new PollingConfig(1L, TimeUnit.HOURS, 1L, TimeUnit.HOURS);
        SdxCluster sdx = getSdxCluster();

        underTest.initAndWaitForStackUpgrade(sdx, pc);

        verify(stackV4Endpoint).upgradeCcmByCrnInternal(any(), eq(STACK_CRN), any());
        verify(cloudbreakPoller).pollCcmUpgradeUntilAvailable(sdx, pc);
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvCrn(ENV_CRN);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setStackCrn(STACK_CRN);
        return sdxCluster;
    }

    private StackV4Response getStack(Tunnel tunnel, Status status) {
        StackV4Response response = new StackV4Response();
        response.setName("stackName");
        response.setTunnel(tunnel);
        response.setStatus(status);
        return response;
    }
}
