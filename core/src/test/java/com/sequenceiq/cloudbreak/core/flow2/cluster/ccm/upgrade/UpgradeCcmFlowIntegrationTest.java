package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_EVENT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
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
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.DeregisterAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.HealthCheckHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.PushSaltStateHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.ReconfigureNginxHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RegisterClusterProxyHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RemoveAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.TunnelUpdateHandler;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.HealthCheckService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.UpgradeCcmOrchestratorService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.exception.FlowNotTriggerableException;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.eventbus.ConsumerCheckerEventBus;
import com.sequenceiq.flow.reactor.handler.ConsumerNotFoundHandler;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.notification.NotificationService;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.core.dispatch.MpscDispatcher;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpgradeCcmFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

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

    @SpyBean
    private UpgradeCcmService upgradeCcmService;

    private Stack stack;

    @BeforeEach
    public void setup() {
        mockStackService();
    }

    @Test
    public void testCcmUpgradeWhenSuccessful() throws CloudbreakOrchestratorException {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
        InOrder inOrder = Mockito.inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService, times(1)).updateTunnel(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).pushSaltState(STACK_ID, CLUSTER_ID);
        inOrder.verify(upgradeCcmService, times(1)).reconfigureNginx(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).registerClusterProxy(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).healthCheck(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).removeAgent(STACK_ID, Tunnel.CCM);
        inOrder.verify(upgradeCcmService, times(1)).deregisterAgent(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmService, never()).ccmUpgradeFailed(STACK_ID, CLUSTER_ID, Tunnel.CCM);
    }

    @Test
    public void testCcmUpgradeWhenTunnelUpdateFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).updateTunnel(STACK_ID);

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
        InOrder inOrder = Mockito.inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService, times(1)).updateTunnel(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).ccmUpgradeFailed(STACK_ID, CLUSTER_ID, Tunnel.CCM);
        verify(upgradeCcmService, never()).pushSaltState(STACK_ID, CLUSTER_ID);
        verify(upgradeCcmService, never()).reconfigureNginx(STACK_ID);
        verify(upgradeCcmService, never()).registerClusterProxy(STACK_ID);
        verify(upgradeCcmService, never()).healthCheck(STACK_ID);
        verify(upgradeCcmService, never()).deregisterAgent(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmService, never()).removeAgent(STACK_ID, Tunnel.CCM);
    }

    @Test
    public void testCcmUpgradeWhenRegisterClusterProxyFail() throws CloudbreakOrchestratorException {
        doThrow(new BadRequestException()).when(upgradeCcmService).registerClusterProxy(STACK_ID);

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
        InOrder inOrder = Mockito.inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService, times(1)).updateTunnel(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).pushSaltState(STACK_ID, CLUSTER_ID);
        inOrder.verify(upgradeCcmService, times(1)).reconfigureNginx(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).registerClusterProxy(STACK_ID);
        inOrder.verify(upgradeCcmService, times(1)).ccmUpgradeFailed(STACK_ID, CLUSTER_ID, Tunnel.CCM);
        verify(upgradeCcmService, never()).healthCheckState(STACK_ID);
        verify(upgradeCcmService, never()).deregisterAgent(STACK_ID, Tunnel.CCM);
        verify(upgradeCcmService, never()).removeAgent(STACK_ID, Tunnel.CCM);
    }

    @Test
    public void testCcmUpgradeTriggerNotAllowed() {
        StackStatus stackStatus = new StackStatus(stack, Status.CREATE_FAILED, "no reason at all",
                DetailedStackStatus.PROVISION_FAILED);
        stack.setStackStatus(stackStatus);
        FlowNotTriggerableException actualException = Assertions.assertThrows(FlowNotTriggerableException.class, this::triggerFlow);
        Assertions.assertTrue(actualException.getMessage().contains("Cluster Connectivity Manager upgrade could not "
                + "be triggered, because the cluster's state is not available."), "FlowNotTriggerableException exception message is not right");
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

    private StackView mockStackView() {
        StackView stackView = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);

        when(stackView.getClusterView()).thenReturn(clusterView);
        when(stackView.getId()).thenReturn(1L);
        when(stackView.isStartInProgress()).thenReturn(true);
        when(clusterView.getId()).thenReturn(CLUSTER_ID);

        return stackView;
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

        return stack;
    }

    private void mockStackService() {
        StackView stackView = mockStackView();
        Stack stack = mockStack();
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stackService.getByIdWithTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector, new UpgradeCcmTriggerRequest(STACK_ID, CLUSTER_ID, Tunnel.CCM)));
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            TransactionService.class,
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
            HealthCheckHandler.class,
            RemoveAgentHandler.class,
            DeregisterAgentHandler.class,
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
        private NotificationService notificationService;

        @MockBean
        private Client client;

        @MockBean
        private SecretService secretService;

        @MockBean
        private FreeIpaV1Endpoint freeIpaV1Endpoint;

        @MockBean
        private Scheduler scheduler;

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

        @Bean
        public io.opentracing.Tracer jaegerTracer() {
            final Reporter reporter = new InMemoryReporter();
            final Sampler sampler = new ConstSampler(false);
            return new JaegerTracer.Builder("untraced-service")
                    .withReporter(reporter)
                    .withSampler(sampler)
                    .build();
        }

        @Bean
        public EventBus reactor(MDCCleanerThreadPoolExecutor threadPoolExecutor, Environment env) {
            MpscDispatcher dispatcher = new MpscDispatcher("test-dispatcher");
            EventBus eventBus = new EventBusSpec()
                    .env(env)
                    .dispatcher(dispatcher)
                    .traceEventPath()
                    .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                    .get();
            return new ConsumerCheckerEventBus(eventBus);
        }
    }

}
