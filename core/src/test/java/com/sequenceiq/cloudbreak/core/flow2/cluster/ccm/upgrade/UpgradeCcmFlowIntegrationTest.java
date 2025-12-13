package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_EVENT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.service.CbEventParameterFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.DeregisterAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.FinalizeHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.PushSaltStateHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.ReconfigureNginxHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RegisterClusterProxyHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RemoveAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RevertAllHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RevertSaltStatesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.TunnelUpdateHandler;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.HealthCheckService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.UpgradeCcmOrchestratorService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.exception.FlowNotTriggerableException;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.notification.WebSocketNotificationService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"cb.ccmRevertJob.activationInMinutes=0"})
class UpgradeCcmFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final int ALL_CALLED_ONCE = 7;

    private static final int UNTIL_TUNNEL_UPDATE = 1;

    private static final int UNTIL_PUSH_SALT_STATES = 2;

    private static final int UNTIL_RECONFIGURE_NGINX = 3;

    private static final int UNTIL_REGISTER_CLUSTER_PROXY_AND_HEALTHCHECK = 4;

    private static final int UNTIL_REMOVE_AGENT = 5;

    private static final int UNTIL_DEREGISTER_AGENT = 6;

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 1234L;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean(reset = MockReset.NONE)
    private StackDtoService stackDtoService;

    @MockBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockBean
    private NodeConfig nodeConfig;

    @SpyBean
    private UpgradeCcmService upgradeCcmService;

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

    private Stack stack;

    @BeforeEach
    public void setup() {
        mockStackService();
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testCcmUpgradeWhenSuccessful() throws CloudbreakOrchestratorException {
        testIt(true, ALL_CALLED_ONCE);
    }

    @Test
    public void testCcmUpgradeWhenTunnelUpdateFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        testIt(false, UNTIL_TUNNEL_UPDATE);
    }

    @Test
    public void testCcmUpgradeWhenPushSaltStatesFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).pushSaltState(STACK_ID, CLUSTER_ID);

        ImmutablePair<InOrder, UpgradeCcmFailedEvent> result = testIt(false, UNTIL_PUSH_SALT_STATES);
        InOrder inOrder = result.left;
        inOrder.verify(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.CCM);
        UpgradeCcmFailedEvent failure = result.right;
        assertTrue(failure.getFailureOrigin().equals(PushSaltStateHandler.class));
        assertNull(failure.getRevertTime());
    }

    @Test
    public void testCcmUpgradeWhenReconfigureNginxFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).reconfigureNginx(STACK_ID);
        ImmutablePair<InOrder, UpgradeCcmFailedEvent> result = testIt(false, UNTIL_RECONFIGURE_NGINX);
        InOrder inOrder = result.left;
        inOrder.verify(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.CCM);
        inOrder.verify(upgradeCcmService).pushSaltState(STACK_ID, CLUSTER_ID);
        UpgradeCcmFailedEvent failure = result.right;
        assertTrue(failure.getFailureOrigin().equals(RevertSaltStatesHandler.class));
        assertNotNull(failure.getRevertTime());
    }

    @Test
    public void testCcmUpgradeWhenRegisterClusterProxyFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).doNothing().when(upgradeCcmService).registerClusterProxyAndCheckHealth(STACK_ID);
        ImmutablePair<InOrder, UpgradeCcmFailedEvent> result = testIt(false, UNTIL_REGISTER_CLUSTER_PROXY_AND_HEALTHCHECK);
        InOrder inOrder = result.left;
        inOrder.verify(upgradeCcmService).updateTunnel(STACK_ID, Tunnel.CCM);
        inOrder.verify(upgradeCcmService).registerClusterProxy(STACK_ID);
        inOrder.verify(upgradeCcmService).healthCheck(STACK_ID);
        inOrder.verify(upgradeCcmService).pushSaltState(STACK_ID, CLUSTER_ID);
        UpgradeCcmFailedEvent failure = result.right;
        assertTrue(failure.getFailureOrigin().equals(RevertAllHandler.class));
        assertNotNull(failure.getRevertTime());
    }

    @Test
    public void testCcmUpgradeWhenRemoveAgentFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).removeAgent(STACK_ID, Tunnel.CCM);
        ImmutablePair<InOrder, UpgradeCcmFailedEvent> result = testIt(true, UNTIL_REMOVE_AGENT);
        InOrder inOrder = result.left;
        inOrder.verify(upgradeCcmService).removeAgentFailed(STACK_ID);
        inOrder.verify(upgradeCcmService).deregisterAgent(STACK_ID, Tunnel.CCM);
    }

    @Test
    public void testCcmUpgradeWhenDeregisterAgentFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).deregisterAgent(STACK_ID, Tunnel.CCM);
        InOrder inOrder = testIt(true, UNTIL_DEREGISTER_AGENT).left;
        inOrder.verify(upgradeCcmService).deregisterAgentFailed(STACK_ID);
    }

    @Test
    public void testCcmUpgradeTriggerNotAllowed() {
        StackStatus stackStatus = new StackStatus(stack, Status.CREATE_FAILED, "no reason at all",
                DetailedStackStatus.PROVISION_FAILED);
        stack.setStackStatus(stackStatus);
        FlowNotTriggerableException actualException = assertThrows(FlowNotTriggerableException.class, this::triggerFlow);
        assertTrue(actualException.getMessage().contains("Cluster Connectivity Manager upgrade could not "
                + "be triggered, because the cluster's state is not available."), "FlowNotTriggerableException exception message is not right");
    }

    public ImmutablePair<InOrder, UpgradeCcmFailedEvent> testIt(boolean success, int calledOnceCount) throws CloudbreakOrchestratorException {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
        UpgradeCcmFailedEvent failure = flowFinished(success);
        return new ImmutablePair<>(verifyServiceCalls(calledOnceCount), failure);
    }

    public UpgradeCcmFailedEvent flowFinished(boolean success) {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");

        ArgumentCaptor<UpgradeCcmFailedEvent> captor = ArgumentCaptor.forClass(UpgradeCcmFailedEvent.class);
        UpgradeCcmFailedEvent result = null;
        verify(upgradeCcmService, success ? never() : times(1)).ccmUpgradeFailed(captor.capture(), eq(CLUSTER_ID));
        if (!success) {
            result = captor.getValue();
        }

        return result;
    }

    public InOrder verifyServiceCalls(int calledOnceCount) throws CloudbreakOrchestratorException {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        int i = 0;

        Tunnel oldTunnel = Tunnel.CCM;
        InOrder inOrder = inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService, times(expected[i++])).updateTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        inOrder.verify(upgradeCcmService, times(expected[i++])).pushSaltState(STACK_ID, CLUSTER_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).reconfigureNginx(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).registerClusterProxyAndCheckHealth(STACK_ID);
        if (calledOnceCount == i) {
            return inOrder;
        }
        inOrder.verify(upgradeCcmService, times(expected[i++])).removeAgent(STACK_ID, oldTunnel);
        if (calledOnceCount == i) {
            return inOrder;
        }
        inOrder.verify(upgradeCcmService, times(expected[i++])).deregisterAgent(STACK_ID, oldTunnel);
        if (calledOnceCount == i) {
            return inOrder;
        }
        inOrder.verify(upgradeCcmService, times(expected[i++])).finalizeUpgrade(STACK_ID);

        return inOrder;
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 10);
    }

    private Stack mockStack() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn");
        stack.setName("stackname");
        StackStatus stackStatus = new StackStatus(stack, Status.AVAILABLE, "no reason at all", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);

        return stack;
    }

    private void mockStackService() {
        Stack stack = mockStack();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(stack.getCluster());
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector, new UpgradeCcmTriggerRequest(STACK_ID, CLUSTER_ID, Tunnel.CCM, null)));
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            TransactionService.class,
            FlowEventCommonListener.class,
            FlowMetricSender.class,
            CommonMetricService.class,
            TransactionMetricsService.class,
            CloudbreakMetricService.class,
            Clock.class,
            CbEventParameterFactory.class,
            ReactorNotifier.class,
            UpgradeCcmActions.class,
            UpgradeCcmFlowConfig.class,
            TunnelUpdateHandler.class,
            PushSaltStateHandler.class,
            ReconfigureNginxHandler.class,
            RegisterClusterProxyHandler.class,
            RemoveAgentHandler.class,
            DeregisterAgentHandler.class,
            RevertSaltStatesHandler.class,
            RevertAllHandler.class,
            FinalizeHandler.class,
            CcmUpgradeFlowTriggerCondition.class,
    })
    @ComponentScan(basePackages = {
            "com.sequenceiq.flow",
    })
    static class Config {

        @MockBean
        private FlowLogRepository flowLogRepository;

        @MockBean
        private FlowChainLogRepository flowChainLogRepository;

        @MockBean
        private OwnerAssignmentService ownerAssignmentService;

        @MockBean
        private WebSocketNotificationService webSocketNotificationService;

        @MockBean
        private Client client;

        @MockBean
        private SecretService secretService;

        @MockBean
        private FreeIpaV1Endpoint freeIpaV1Endpoint;

        @MockBean
        private TransactionalScheduler scheduler;

        @MockBean
        private FlowOperationStatisticsService flowOperationStatisticsService;

        @MockBean
        private StackStatusFinalizer stackStatusFinalizer;

        @MockBean
        private ClusterService clusterService;

        @MockBean
        private StackUpdater stackUpdater;

        @MockBean
        private CloudbreakFlowMessageService flowMessageService;

        @MockBean
        private StackUtil stackUtil;

        @MockBean
        private InstanceMetaDataService instanceMetaDataService;

        @MockBean
        private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

        @MockBean
        private CloudbreakFlowInformation cloudbreakFlowInformation;

        @MockBean
        private ClusterServiceRunner clusterServiceRunner;

        @MockBean
        private UpgradeCcmOrchestratorService upgradeCcmOrchestratorService;

        @MockBean
        private CcmResourceTerminationListener ccmResourceTerminationListener;

        @MockBean
        private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

        @MockBean
        private ClusterProxyService clusterProxyService;

        @MockBean
        private HealthCheckService healthCheckService;

        @MockBean
        private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

        @Bean
        public EventBus reactor(ExecutorService threadPoolExecutor) {
            return EventBus.builder()
                    .executor(threadPoolExecutor)
                    .exceptionHandler((exception, context) -> {
                    })
                    .unhandledEventHandler(event -> {
                    })
                    .build();
        }

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any())).thenReturn(
                    Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }

}
