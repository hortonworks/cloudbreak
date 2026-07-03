package com.sequenceiq.freeipa.flow.freeipa.downscale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackCollectResourcesHandler;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.handler.CertRevokeHandler;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.handler.DnsRemoveHandler;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaValidationProperties;
import com.sequenceiq.freeipa.flow.freeipa.downscale.action.FreeIpaDownscaleActions;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.CollectAdditionalHostnamesHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.RemoveHostsHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.RemoveReplicationAgreementsHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.ServerRemoveHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.StopTelemetryHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.UpdateDnsSoaRecordsHandler;
import com.sequenceiq.freeipa.flow.freeipa.downscale.handler.VerifyReplicationCleanupHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.ClusterProxyUpdateRegistrationHandler;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.VerifyReplicationCleanupService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsSoaRecordService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaTopologyService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryAgentService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class FreeIpaDownscaleFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "operationId";

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "env-crn";

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private TerminationService terminationService;

    @MockBean
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @MockBean
    private PollTaskFactory pollTaskFactory;

    @MockBean
    private ClusterProxyService clusterProxyService;

    @MockBean
    private FreeIpaClientFactory freeIpaClientFactory;

    @MockBean
    private TelemetryAgentService telemetryAgentService;

    @MockBean
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @MockBean
    private InstanceGroupService instanceGroupService;

    @MockBean
    private CleanupService cleanupService;

    @MockBean
    private FreeIpaTopologyService freeIpaTopologyService;

    @Inject
    private FreeIpaService freeIpaService;

    @MockBean
    private DnsSoaRecordService dnsSoaRecordService;

    @MockBean
    private HostOrchestrator hostOrchestrator;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @MockBean
    private BootstrapService bootstrapService;

    @MockBean
    private VerifyReplicationCleanupService verifyReplicationCleanupService;

    @MockBean
    private KerberosConfigUpdateService kerberosConfigUpdateService;

    @MockBean
    private EnvironmentEndpoint environmentEndpoint;

    @MockBean
    private OperationService operationService;

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private NodeConfig nodeConfig;

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

    @MockBean
    private EventSenderService eventSenderService;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @MockBean
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @BeforeEach
    public void setup() throws Exception {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        InstanceGroup ig = new InstanceGroup();
        ig.setInstanceMetaData(Set.of());
        stack.setInstanceGroups(Set.of(ig));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);

        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.collectResourcesToRemove(any(), any(), any(), any())).thenReturn(List.of());
        when(resourceConnector.downscale(any(), any(), any(), any(), any())).thenReturn(List.of());
        PollTask<ResourcesStatePollerResult> pollTask = mock(PollTask.class);
        when(pollTaskFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(pollTask.completed(any())).thenReturn(Boolean.TRUE);

        when(stackToCloudStackConverter.convert(stack)).thenReturn(mock(CloudStack.class));
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(mock(CloudCredential.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(List.of());

        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllServers()).thenReturn(Set.of());

        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(new DetailedEnvironmentResponse());
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        when(cleanupService.removeServers(eq(STACK_ID), any())).thenReturn(Pair.of(Set.of(), Map.of()));
        when(cleanupService.revokeCerts(eq(STACK_ID), any())).thenReturn(Pair.of(Set.of(), Map.of()));
        when(cleanupService.removeDnsEntries(eq(STACK_ID), any(), any(), any(), any())).thenReturn(Pair.of(Set.of(), Map.of()));

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("example.com");
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa);

        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(Set.of());

        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testDownscaleFlowCompletesWithReplicationCleanupState() throws Exception {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertTrue(flowRegister.getRunningFlowIds().isEmpty(), "Flow should have finished");
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, atLeastOnce()).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "Flow should be finalized");
        verify(verifyReplicationCleanupService).verifyOnSurvivingMasters(eq(STACK_ID), any());
        verify(stackUpdater).updateStackStatus(any(Stack.class), eq(DetailedStackStatus.DOWNSCALE_COMPLETED), eq("Downscale complete"));
    }

    private FlowIdentifier triggerFlow() {
        String selector = DownscaleFlowEvent.DOWNSCALE_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new DownscaleEvent(selector, STACK_ID, List.of(), 1, false, false, false, OPERATION_ID)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 50);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            FreeIpaDownscaleActions.class,
            DownscaleFlowConfig.class,
            FlowIntegrationTestConfig.class,
            CloudPlatformInitializer.class,
            ResourceToCloudResourceConverter.class,
            ResourceAttributeUtil.class,
            WebApplicationExceptionMessageExtractor.class,
            FreeIpaFailedFlowAnalyzer.class,
            FreeIpaValidationProperties.class,
            ClusterProxyUpdateRegistrationHandler.class,
            CollectAdditionalHostnamesHandler.class,
            StopTelemetryHandler.class,
            DownscaleStackCollectResourcesHandler.class,
            DownscaleStackHandler.class,
            ServerRemoveHandler.class,
            RemoveReplicationAgreementsHandler.class,
            CertRevokeHandler.class,
            DnsRemoveHandler.class,
            UpdateDnsSoaRecordsHandler.class,
            RemoveHostsHandler.class,
            VerifyReplicationCleanupHandler.class
    })
    static class TestConfig {

        @MockBean
        private FreeipaJobService jobService;
    }
}
