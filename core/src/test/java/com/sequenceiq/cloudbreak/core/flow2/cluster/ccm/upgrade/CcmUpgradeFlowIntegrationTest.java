package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeEvent.CCM_UPGRADE_EVENT;
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
import org.quartz.Scheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.service.CbEventParameterFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.CcmRemoveAutoSshHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.CcmReregisterClusterToClusterProxyHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.CcmUnregisterHostsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.CcmUpgradePreparationHandler;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
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

@ExtendWith(SpringExtension.class)
class CcmUpgradeFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @SpyBean
    private CcmUpgradeService ccmUpgradeService;

    private Stack stack;

    @BeforeEach
    public void setup() {
        mockStackService();
    }

    @Test
    public void testCcmUpgradeWhenSuccessful() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
        verify(ccmUpgradeService, times(1)).reregister(STACK_ID);
        verify(ccmUpgradeService, times(1)).unregister(STACK_ID);
        verify(ccmUpgradeService, times(1)).prepare(STACK_ID);
        verify(ccmUpgradeService, times(1)).removeAutoSsh(STACK_ID);
        verify(ccmUpgradeService, never()).ccmUpgradeFailed(STACK_ID);
    }

    @Test
    public void testCcmUpgradeWhenPrepFail() {
        doThrow(new BadRequestException()).when(ccmUpgradeService).prepare(STACK_ID);

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
        verify(ccmUpgradeService, times(1)).prepare(STACK_ID);
        verify(ccmUpgradeService, times(1)).ccmUpgradePreparationFailed(STACK_ID);
        verify(ccmUpgradeService, never()).ccmUpgradeFailed(STACK_ID);
        verify(ccmUpgradeService, never()).reregister(STACK_ID);
        verify(ccmUpgradeService, never()).unregister(STACK_ID);
        verify(ccmUpgradeService, never()).removeAutoSsh(STACK_ID);
    }

    @Test
    public void testCcmUpgradeWhenReRegisterFail() {
        doThrow(new BadRequestException()).when(ccmUpgradeService).reregister(STACK_ID);

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        Assertions.assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
        verify(ccmUpgradeService, times(1)).prepare(STACK_ID);
        verify(ccmUpgradeService, times(1)).ccmUpgradeFailed(STACK_ID);
        verify(ccmUpgradeService, times(1)).reregister(STACK_ID);
        verify(ccmUpgradeService, never()).ccmUpgradePreparationFailed(STACK_ID);
        verify(ccmUpgradeService, never()).unregister(STACK_ID);
        verify(ccmUpgradeService, never()).removeAutoSsh(STACK_ID);
    }

    @Test
    public void testCcmUpgradeTriggerNotAllowed() {
        StackStatus stackStatus = new StackStatus(stack, Status.CREATE_FAILED, "no reason at all",
                DetailedStackStatus.PROVISION_FAILED);
        stack.setStackStatus(stackStatus);
        FlowNotTriggerableException actualException = Assertions.assertThrows(FlowNotTriggerableException.class, () -> triggerFlow());
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

        return stackView;
    }

    private Stack mockStack() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("stackname");
        StackStatus stackStatus = new StackStatus(stack, Status.AVAILABLE, "no reason at all", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        stack.setCluster(new Cluster());

        return stack;
    }

    private void mockStackService() {
        StackView stackView = mockStackView();
        Stack stack = mockStack();
        when(stackService.getByIdWithTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getViewByIdWithoutAuth(STACK_ID)).thenReturn(stackView);
    }

    private FlowIdentifier triggerFlow() {
        String selector = CCM_UPGRADE_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector, new StackEvent(selector, STACK_ID)));
    }

    @TestConfiguration
    @Import({
            TransactionService.class,
            TransactionMetricsService.class,
            Clock.class,
            CbEventParameterFactory.class,
            ReactorNotifier.class,
            CcmUpgradeActions.class,
            CcmUpgradeFlowConfig.class,
            CcmRemoveAutoSshHandler.class,
            CcmReregisterClusterToClusterProxyHandler.class,
            CcmUnregisterHostsHandler.class,
            CcmUpgradePreparationHandler.class,
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