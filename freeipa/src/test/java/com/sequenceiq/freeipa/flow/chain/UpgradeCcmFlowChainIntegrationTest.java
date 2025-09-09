package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataFlowConfig;
import com.sequenceiq.freeipa.flow.stack.update.action.UserDataUpdateActions;
import com.sequenceiq.freeipa.flow.stack.update.handler.UpdateUserDataHandler;
import com.sequenceiq.freeipa.flow.stack.update.handler.UpdateUserDataOnProviderHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmFlowConfig;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmOperationAcceptor;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmActions;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmContext;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmChangeTunnelHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmCheckPrerequisitesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmDeregisterMinaHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmFinalizingHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmObtainAgentDataHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmPushSaltStatesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmReconfigureNginxHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRegisterClusterProxyHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRemoveMinaHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmUpgradeHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.upgrade.ccm.CcmParametersConfigService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpgradeCcmFlowChainIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String USER_DATA = "hello hello is there anybody out there";

    private static final Map<InstanceGroupType, String> USER_DATA_MAP = Map.of(InstanceGroupType.GATEWAY, USER_DATA);

    private static final long STACK_ID = 1L;

    private static final int ALL_CALLED_ONCE = 21;

    private static final int CALLED_ONCE_TILL_GENERATE_USERDATA = 19;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private UpgradeCcmService upgradeCcmService;

    @MockBean
    private OperationService operationService;

    @MockBean
    private UserDataService userDataService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private StackToCloudStackConverter cloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialConverter;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockBean
    private NodeConfig nodeConfig;

    @SpyBean
    private FlowChains flowChains;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @MockBean
    private StackStatusFinalizer stackStatusFinalizer;

    @Mock
    private ResourceConnector resourcesApi;

    @BeforeEach
    public void setup() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(Tunnel.CCM);
        ImageEntity image = new ImageEntity();
        stack.setImage(image);
        image.setUserdata(USER_DATA);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        CloudConnector connector = mock(CloudConnector.class);
        AuthenticatedContext context = mock(AuthenticatedContext.class);
        Authenticator authApi = mock(Authenticator.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authApi);
        when(connector.resources()).thenReturn(resourcesApi);
        when(authApi.authenticate(any(), any())).thenReturn(context);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testCcmUpgradeFlowChainWhenSuccessful() throws Exception {
        testFlow(ALL_CALLED_ONCE, true, true);
    }

    @Test
    public void testUpdateUserDataFailsInChain() throws Exception {
        doThrow(new BadRequestException()).when(userDataService).regenerateUserDataForCcmUpgrade(STACK_ID);
        testFlow(CALLED_ONCE_TILL_GENERATE_USERDATA, true, false);
    }

    @Test
    public void testCcmUpgradeWhenDeregisterMinaFailsInChain() throws Exception {
        doThrow(new BadRequestException()).when(upgradeCcmService).deregisterMina(STACK_ID);
        testFlow(ALL_CALLED_ONCE, true, true, false);
    }

    private void testFlow(int calledOnce, boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess) throws Exception {
        testFlow(calledOnce, ccmUpgradeSuccess, userDataUpdateSuccess, true);
    }

    private void testFlow(int calledOnce, boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess, boolean minaRemoved) throws Exception {
        triggerFlow();
        letItFlow();

        flowFinishedSuccessfully(ccmUpgradeSuccess ? 2 : 1);
        verifyServiceCalls(calledOnce);
        verifyFinishingStatCalls(ccmUpgradeSuccess, userDataUpdateSuccess, minaRemoved);
    }

    private void verifyFinishingStatCalls(boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess, boolean minaRemoved) throws Exception {
        verify(upgradeCcmService, times(ccmUpgradeSuccess ? 1 : 0)).finishedState(STACK_ID, minaRemoved);
        verify(resourcesApi, times(userDataUpdateSuccess ? 1 : 0)).updateUserData(any(), any(), any(), eq(USER_DATA_MAP));

        ArgumentCaptor<UpgradeCcmContext> contextCaptor = ArgumentCaptor.forClass(UpgradeCcmContext.class);
        ArgumentCaptor<UpgradeCcmFailureEvent> payloadCaptor = ArgumentCaptor.forClass(UpgradeCcmFailureEvent.class);
        verify(upgradeCcmService, times(ccmUpgradeSuccess ? 0 : 1)).failedState(contextCaptor.capture(), payloadCaptor.capture());
        if (!ccmUpgradeSuccess) {
            UpgradeCcmContext context = contextCaptor.getValue();
            UpgradeCcmFailureEvent payload = payloadCaptor.getValue();
            assertEquals(STACK_ID, context.getStack().getId());
            assertEquals(STACK_ID, payload.getResourceId());
        }

        verify(operationService, times(ccmUpgradeSuccess && userDataUpdateSuccess ? 1 : 0)).completeOperation(any(), any(), any(), any());
        verify(operationService, times(ccmUpgradeSuccess && userDataUpdateSuccess ? 0 : 1)).failOperation(any(), any(), any());

    }

    private void verifyServiceCalls(int calledOnceCount) throws Exception {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        int i = 0;
        InOrder inOrder = Mockito.inOrder(upgradeCcmService, userDataService, resourcesApi);
        inOrder.verify(upgradeCcmService, times(expected[i++])).checkPrerequisitesState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).checkPrerequsities(STACK_ID, Tunnel.CCM);
        inOrder.verify(upgradeCcmService, times(expected[i++])).changeTunnelState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).changeTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        inOrder.verify(upgradeCcmService, times(expected[i++])).obtainAgentDataState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).obtainAgentData(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).pushSaltStatesState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).pushSaltStates(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).upgradeState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).upgrade(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).reconfigureNginxState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).reconfigureNginx(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).registerClusterProxyState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).registerClusterProxyAndCheckHealth(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).removeMinaState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).removeMina(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).deregisterMinaState(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).deregisterMina(STACK_ID);
        inOrder.verify(userDataService, times(expected[i++])).regenerateUserDataForCcmUpgrade(STACK_ID);
        inOrder.verify(resourcesApi, times(expected[i++])).updateUserData(any(), any(), any(), eq(USER_DATA_MAP));
    }

    private void flowFinishedSuccessfully(int numberOfRanFlows) {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2 * numberOfRanFlows)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new UpgradeCcmFlowChainTriggerEvent(selector, "opi", STACK_ID, Tunnel.CCM)));
    }

    private void letItFlow() {
        verify(flowChains, timeout(50000).atLeastOnce()).removeFlowChain(anyString(), anyBoolean());
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            UpgradeCcmFlowEventChainFactory.class,

            UpdateUserDataFlowConfig.class,
            UserDataUpdateActions.class,
            UpdateUserDataHandler.class,
            UpdateUserDataOnProviderHandler.class,
            UpgradeCcmOperationAcceptor.class,
            UpgradeCcmFlowConfig.class,
            UpgradeCcmActions.class,
            UpgradeCcmCheckPrerequisitesHandler.class,
            UpgradeCcmChangeTunnelHandler.class,
            UpgradeCcmPushSaltStatesHandler.class,
            UpgradeCcmUpgradeHandler.class,
            UpgradeCcmReconfigureNginxHandler.class,
            UpgradeCcmRegisterClusterProxyHandler.class,
            UpgradeCcmRemoveMinaHandler.class,
            UpgradeCcmObtainAgentDataHandler.class,
            UpgradeCcmDeregisterMinaHandler.class,
            UpgradeCcmFinalizingHandler.class,
            UpgradeCcmService.class,
            FlowIntegrationTestConfig.class
    })
    static class Config {
        @MockBean
        private CcmParametersConfigService ccmParametersConfigService;

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any())).thenReturn(
                    Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
