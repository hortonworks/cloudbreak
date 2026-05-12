package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action.UpdateTrustedRealmAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action.UpdateTrustedRealmFailedAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action.UpdateTrustedRealmFinishedAction;
import com.sequenceiq.cloudbreak.core.flow2.service.CbEventParameterFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.trustedrealm.UpdateTrustedRealmHandler;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterServiceConfigurationLookup;
import com.sequenceiq.cloudbreak.service.cluster.ClusterServiceConfigurationUpdate;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
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
class UpdateTrustedRealmFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 1234L;

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resourceCrn";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn";

    private static final String REALM = "EXAMPLE.COM";

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
    private UpdateTrustedRealmStatusService updateTrustedRealmStatusService;

    @SpyBean
    private ClusterService clusterService;

    private Stack stack;

    @BeforeEach
    void setup() {
        stack = mockStack();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(stack.getCluster());
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(STACK_CRN)), any())).thenReturn(stackDto);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    void testUpdateTrustedRealmWhenRealmNotYetConfigured() {
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class))).thenReturn(Optional.empty());
        doNothing().when(clusterService).updateClusterServiceConfiguration(any(), any(ClusterServiceConfigurationUpdate.class));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, false);
        verify(clusterService, times(1)).getClusterServiceConfigValue(eq(NameOrCrn.ofCrn(STACK_CRN)), any());
        verify(clusterService, times(1)).updateClusterServiceConfiguration(eq(NameOrCrn.ofCrn(STACK_CRN)), any(ClusterServiceConfigurationUpdate.class));
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testUpdateTrustedRealmWhenRealmAlreadyConfigured() {
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class)))
                .thenReturn(Optional.of(REALM));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, false);
        verify(clusterService, times(1)).getClusterServiceConfigValue(eq(NameOrCrn.ofCrn(STACK_CRN)), any());
        verify(clusterService, never()).updateClusterServiceConfiguration(any(), any());
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testUpdateTrustedRealmWhenRealmIsAppendedToExistingRealms() {
        String existingRealm = "OTHER.COM";
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class)))
                .thenReturn(Optional.of(existingRealm));
        doNothing().when(clusterService).updateClusterServiceConfiguration(any(), any(ClusterServiceConfigurationUpdate.class));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        ArgumentCaptor<ClusterServiceConfigurationUpdate> updateCaptor = ArgumentCaptor.forClass(ClusterServiceConfigurationUpdate.class);
        verify(clusterService, times(1)).updateClusterServiceConfiguration(any(), updateCaptor.capture());
        ClusterServiceConfigurationUpdate update = updateCaptor.getValue();
        String updatedValue = update.getServiceConfigurations().get(0).getValue();
        assertTrue(updatedValue.contains(existingRealm), "Updated value should still contain the existing realm");
        assertTrue(updatedValue.contains(REALM), "Updated value should contain the new realm");
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testUpdateTrustedRealmWhenHandlerFails() {
        doThrow(new RuntimeException("CM not reachable"))
                .when(clusterService).getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, false);
        verify(clusterService, never()).updateClusterServiceConfiguration(any(), any());
        verify(updateTrustedRealmStatusService, never()).success(STACK_ID);
        verify(updateTrustedRealmStatusService, times(1)).failed(eq(STACK_ID), any(Exception.class), eq(false));
    }

    @Test
    void testUpdateTrustedRealmWhenUpdateCallFails() {
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class))).thenReturn(Optional.empty());
        doThrow(new RuntimeException("CM update failed"))
                .when(clusterService).updateClusterServiceConfiguration(any(), any(ClusterServiceConfigurationUpdate.class));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, false);
        verify(updateTrustedRealmStatusService, never()).success(STACK_ID);
        verify(updateTrustedRealmStatusService, times(1)).failed(eq(STACK_ID), any(Exception.class), eq(false));
    }

    @Test
    void testRemoveTrustedRealmWhenRealmPresent() {
        String existingRealms = "OTHER.COM," + REALM;
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class)))
                .thenReturn(Optional.of(existingRealms));
        doNothing().when(clusterService).updateClusterServiceConfiguration(any(), any(ClusterServiceConfigurationUpdate.class));

        FlowIdentifier flowIdentifier = triggerFlow(true);
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, true);
        ArgumentCaptor<ClusterServiceConfigurationUpdate> updateCaptor = ArgumentCaptor.forClass(ClusterServiceConfigurationUpdate.class);
        verify(clusterService, times(1)).updateClusterServiceConfiguration(eq(NameOrCrn.ofCrn(STACK_CRN)), updateCaptor.capture());
        String updatedValue = updateCaptor.getValue().getServiceConfigurations().get(0).getValue();
        assertTrue(updatedValue.contains("OTHER.COM"), "Updated value should still contain the other realm");
        assertFalse(updatedValue.contains(REALM), "Updated value should not contain the removed realm");
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testRemoveTrustedRealmWhenLastRealmIsRemoved() {
        // Only the realm being removed is configured; after removal CM should revert to default (null value).
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class)))
                .thenReturn(Optional.of(REALM));
        doNothing().when(clusterService).updateClusterServiceConfiguration(any(), any(ClusterServiceConfigurationUpdate.class));

        FlowIdentifier flowIdentifier = triggerFlow(true);
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, true);
        ArgumentCaptor<ClusterServiceConfigurationUpdate> updateCaptor = ArgumentCaptor.forClass(ClusterServiceConfigurationUpdate.class);
        verify(clusterService, times(1)).updateClusterServiceConfiguration(eq(NameOrCrn.ofCrn(STACK_CRN)), updateCaptor.capture());
        String updatedValue = updateCaptor.getValue().getServiceConfigurations().get(0).getValue();
        assertNull(updatedValue, "Value should be null so CM reverts trusted_realms to its default");
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testRemoveTrustedRealmWhenRealmNotPresent() {
        when(clusterService.getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class)))
                .thenReturn(Optional.of("OTHER.COM"));

        FlowIdentifier flowIdentifier = triggerFlow(true);
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, true);
        verify(clusterService, never()).updateClusterServiceConfiguration(any(), any());
        verify(updateTrustedRealmStatusService, times(1)).success(STACK_ID);
        verify(updateTrustedRealmStatusService, never()).failed(any(), any(), anyBoolean());
    }

    @Test
    void testRemoveTrustedRealmWhenHandlerFails() {
        doThrow(new RuntimeException("CM not reachable"))
                .when(clusterService).getClusterServiceConfigValue(any(), any(ClusterServiceConfigurationLookup.class));

        FlowIdentifier flowIdentifier = triggerFlow(true);
        letItFlow(flowIdentifier);

        assertFlowFinalized();
        verify(updateTrustedRealmStatusService, times(1)).updatingTrustedRealm(STACK_ID, true);
        verify(clusterService, never()).updateClusterServiceConfiguration(any(), any());
        verify(updateTrustedRealmStatusService, never()).success(STACK_ID);
        verify(updateTrustedRealmStatusService, times(1)).failed(eq(STACK_ID), any(Exception.class), eq(true));
    }

    private void assertFlowFinalized() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "Flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        return triggerFlow(false);
    }

    private FlowIdentifier triggerFlow(boolean remove) {
        String selector = UPDATE_TRUSTED_REALM_TRIGGER_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector,
                        new UpdateTrustedRealmTriggerEvent(selector, STACK_ID, STACK_CRN, ENV_CRN, REALM, remove, null)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 10);
    }

    private Stack mockStack() {
        Stack s = new Stack();
        s.setId(STACK_ID);
        s.setResourceCrn(STACK_CRN);
        s.setName("stackname");
        StackStatus stackStatus = new StackStatus(s, Status.AVAILABLE, "no reason at all", DetailedStackStatus.AVAILABLE);
        s.setStackStatus(stackStatus);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        s.setCluster(cluster);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        s.setWorkspace(workspace);
        return s;
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
            UpdateTrustedRealmFlowConfig.class,
            UpdateTrustedRealmAction.class,
            UpdateTrustedRealmFinishedAction.class,
            UpdateTrustedRealmFailedAction.class,
            UpdateTrustedRealmStatusService.class,
            UpdateTrustedRealmHandler.class,
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
        private ClusterApiConnectors clusterApiConnectors;

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

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory factory = mock(CommonExecutorServiceFactory.class);
            when(factory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any()))
                    .thenReturn(Executors.newCachedThreadPool());
            return factory;
        }
    }
}


