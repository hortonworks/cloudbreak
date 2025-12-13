package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateFlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.migrate.MigrateRdsCertificateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.migrate.SetupNonTlsToTlsHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.CheckRotateRdsCertificatePrerequisitesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.GetLatestRdsCertificateHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.RestartCmHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.RollingRestartServicesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.RotateRdsCertificateOnProviderHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate.UpdateLatestRdsCertificateHandler;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
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
class RotateRdsCertificateFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final int ALL_CALLED_ONCE = 7;

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

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private FlowCancelService flowCancelService;

    @SpyBean
    private RotateRdsCertificateService rotateRdsCertificateService;

    @SpyBean
    private MigrateRdsCertificateService migrateRdsCertificateService;

    private Stack stack;

    @BeforeEach
    public void setup() {
        mockStackService();
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testRotateRdsCertificateWhenSuccessful() throws CloudbreakOrchestratorException {
        testIt(true, ALL_CALLED_ONCE);
    }

    private ImmutablePair<InOrder, RotateRdsCertificateFailedEvent> testIt(boolean success, int calledOnceCount) throws CloudbreakOrchestratorException {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);
        RotateRdsCertificateFailedEvent failure = flowFinished(success);
        return new ImmutablePair<>(verifyServiceCalls(calledOnceCount), failure);
    }

    private RotateRdsCertificateFailedEvent flowFinished(boolean success) {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");

        ArgumentCaptor<RotateRdsCertificateFailedEvent> captor = ArgumentCaptor.forClass(RotateRdsCertificateFailedEvent.class);
        RotateRdsCertificateFailedEvent result = null;
        verify(rotateRdsCertificateService, success ? never() : times(1)).rotateRdsCertFailed(captor.capture());
        if (!success) {
            result = captor.getValue();
        }

        return result;
    }

    private InOrder verifyServiceCalls(int calledOnceCount) throws CloudbreakOrchestratorException {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        int i = 0;

        InOrder inOrder = inOrder(rotateRdsCertificateService);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).checkPrerequisitesState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).checkPrerequisites(STACK_ID, ROTATE);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).getLatestRdsCertificateState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).getLatestRdsCertificate(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).updateLatestRdsCertificateState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).updateLatestRdsCertificate(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).restartCmState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).restartCm(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).rollingRestartRdsCertificateState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).rollingRestartServices(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i])).rotateOnProviderState(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).rotateOnProvider(STACK_ID);
        inOrder.verify(rotateRdsCertificateService, times(expected[i++])).rotateRdsCertFinished(STACK_ID);
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
        stack.setType(StackType.WORKLOAD);
        return stack;
    }

    private void mockStackService() {
        Stack stack = mockStack();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(stack.getCluster());
        doNothing().when(migrateRdsCertificateService).setupNonTlsToTlsIfRequired(anyLong());
    }

    private FlowIdentifier triggerFlow() {
        String selector = ROTATE_RDS_CERTIFICATE_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector, new RotateRdsCertificateTriggerRequest(STACK_ID, ROTATE)));
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            TransactionService.class,
            CommonMetricService.class,
            TransactionMetricsService.class,
            CloudbreakMetricService.class,
            Clock.class,
            CbEventParameterFactory.class,
            ReactorNotifier.class,
            RotateRdsCertificateActions.class,
            RotateRdsCertificateFlowConfig.class,
            MigrateRdsCertificateService.class,
            RotateRdsCertificateFlowTriggerCondition.class,
            CheckRotateRdsCertificatePrerequisitesHandler.class,
            GetLatestRdsCertificateHandler.class,
            SetupNonTlsToTlsHandler.class,
            RestartCmHandler.class,
            UpdateLatestRdsCertificateHandler.class,
            RollingRestartServicesHandler.class,
            RotateRdsCertificateOnProviderHandler.class,
            ClusterApiConnectors.class,
            TlsSecurityService.class,
            SecurityConfigService.class,
            SecurityConfigRepository.class,
            RotateRdsCertificateService.class
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
        private ClusterProxyService clusterProxyService;

        @MockBean
        private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

        @MockBean
        private MeterRegistry meterRegistry;

        @MockBean
        private TlsSecurityService tlsSecurityService;

        @MockBean
        private ClusterApiConnectors clusterApiConnectors;

        @MockBean
        private SecurityConfigService securityConfigService;

        @MockBean
        private SecurityConfigRepository securityConfigRepository;

        @MockBean
        private RotateRdsCertificateService rotateRdsCertificateService;

        @MockBean
        private MigrateRdsCertificateService migrateRdsCertificateService;

        @MockBean
        private FlowEventCommonListener flowEventCommonListener;

        @MockBean
        private FlowEventListener flowEventListener;

        @MockBean
        private FlowUsageSender flowUsageSender;

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
    }

}
