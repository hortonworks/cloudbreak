package com.sequenceiq.freeipa.flow.freeipa.rebuild;

import static com.sequenceiq.common.api.type.Tunnel.CLUSTER_PROXY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.handler.CollectMetadataHandler;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackCollectResourcesHandler;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackHandler;
import com.sequenceiq.cloudbreak.cloud.handler.RebootInstanceHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientCallable;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.BootstrapMachineHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.InstallFreeIpaServicesHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.OrchestratorConfigHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.PostInstallFreeIpaHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.ValidateCloudStorageHandler;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildAddInstanceAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildBootstrapMachineAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildCleanupFreeIpaAfterRestoreAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildCollectResourcesAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildExtendMetadataAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildInstallFreeIpaAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildOrchestratorConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildPostInstallAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRebootAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRebootWaitUntilAvailableAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRegisterClusterProxyAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRemoveInstancesAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRemoveInstancesFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildRestoreFreeIpaAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildSaveMetadataAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildStartAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildTlsSetupAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateEnvironmentStackConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateKerberosNameServersConfigAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateMetadataForDeletionAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateBackupAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateCloudStorageAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateHealthAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildValidateInstanceAction;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.handler.FreeIpaCleanupAfterRestoreHandler;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.handler.FreeIpaRestoreHandler;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.handler.RebuildValidateHealthHandler;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.handler.ValidateBackupHandler;
import com.sequenceiq.freeipa.flow.freeipa.upscale.action.PrivateIdProvider;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.FreeipaUpscaleStackHandler;
import com.sequenceiq.freeipa.flow.stack.HealthCheckHandler;
import com.sequenceiq.freeipa.flow.stack.provision.action.StackProvisionService;
import com.sequenceiq.freeipa.flow.stack.provision.handler.ClusterProxyRegistrationHandler;
import com.sequenceiq.freeipa.flow.stack.start.FreeIpaServiceStartService;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientRetryService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaCloudStorageValidationService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaInstallService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaPostInstallService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceValidationService;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;
import com.sequenceiq.freeipa.sync.StackStatusCheckerJob;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "cb.max.salt.restore.dl_and_validate.retry=90",
        "cb.max.salt.restore.dl_and_validate.retry.onerror=5"
})
class RebuildFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private ResourceConnector resourceConnector;

    @MockBean
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @MockBean
    private PollTaskFactory statusCheckFactory;

    @MockBean
    private ClusterProxyService clusterProxyService;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private BootstrapService bootstrapService;

    @MockBean
    private FreeIpaOrchestrationConfigService orchestrationConfigService;

    @MockBean
    private FreeIpaCloudStorageValidationService storageValidationService;

    @MockBean
    private FreeIpaInstallService freeIpaInstallService;

    @MockBean
    private FreeIpaPostInstallService freeIpaPostInstallService;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private TerminationService terminationService;

    @MockBean
    private NodeConfig nodeConfig;

    @MockBean
    private PrivateIdProvider privateIdProvider;

    @MockBean
    private InstanceValidationService instanceValidationService;

    @MockBean
    private MetadataSetupService metadataSetupService;

    @MockBean
    private StackProvisionService stackProvisionService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private HostOrchestrator hostOrchestrator;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @MockBean
    private FreeIpaServiceStartService freeIpaServiceStartService;

    @MockBean
    private StackStatusCheckerJob stackStatusCheckerJob;

    @MockBean
    private CleanupService cleanupService;

    @MockBean
    private FreeIpaClientFactory freeIpaClientFactory;

    @MockBean
    private FreeIpaClientRetryService retryService;

    @MockBean
    private FreeIpaSafeInstanceHealthDetailsService healthDetailsService;

    @MockBean
    private KerberosConfigUpdateService kerberosConfigUpdateService;

    @MockBean
    private EnvironmentEndpoint environmentEndpoint;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private FreeIpaService freeIpaService;

    @BeforeEach
    public void setup() throws FreeIpaClientException, QuotaExceededException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(CLUSTER_PROXY);
        InstanceGroup ig = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetaData.setDiscoveryFQDN("ipaserver0.example.com");
        ig.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(ig));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(mock(Authenticator.class));
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)))).thenAnswer(inv -> {
            instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            return List.of();
        });
        PollTask<ResourcesStatePollerResult> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(pollTask.completed(any())).thenReturn(Boolean.TRUE);
        when(cloudConnector.metadata()).thenReturn(mock(MetadataCollector.class));
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(new InstanceMetaData()));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn("ipaserver0.example.com");
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(stackToCloudStackConverter.buildInstance(eq(stack), any(InstanceMetaData.class), eq(ig), any(), any(), any()))
                .thenReturn(mock(CloudInstance.class));
        when(cloudConnector.instances()).thenReturn(mock(InstanceConnector.class));
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(mock(FreeIpaClient.class));
        lenient().doAnswer(invocation -> invocation.getArgument(0, FreeIpaClientCallable.class).run())
                .when(retryService).retryWhenRetryableWithValue(any(FreeIpaClientCallable.class));
        when(instanceMetaDataToCloudInstanceConverter.convert(instanceMetaData)).thenReturn(mock(CloudInstance.class));
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("example.com");
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa);
        NodeHealthDetails healthDetails = new NodeHealthDetails();
        healthDetails.setStatus(InstanceStatus.CREATED);
        when(healthDetailsService.getInstanceHealthDetails(stack, instanceMetaData)).thenReturn(healthDetails);
        when(regionAwareInternalCrnGeneratorFactory.iam())
                .thenReturn(RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator(Crn.Service.IAM, "cdp", "us-west-1"));
    }

    @Test
    public void testRebuildWhenSuccessful() {
        testFlow();
    }

    private void testFlow() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        RebuildEvent rebuildEvent = new RebuildEvent(STACK_ID, "", "", "");
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(rebuildEvent.selector(), rebuildEvent));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import(value = {
            RebuildStartAction.class,
            RebuildUpdateMetadataForDeletionAction.class,
            RebuildCollectResourcesAction.class,
            RebuildRemoveInstancesAction.class,
            RebuildRemoveInstancesFinishedAction.class,
            RebuildAddInstanceAction.class,
            RebuildValidateInstanceAction.class,
            RebuildExtendMetadataAction.class,
            RebuildSaveMetadataAction.class,
            RebuildTlsSetupAction.class,
            RebuildRegisterClusterProxyAction.class,
            RebuildBootstrapMachineAction.class,
            RebuildOrchestratorConfigAction.class,
            RebuildValidateBackupAction.class,
            RebuildValidateCloudStorageAction.class,
            RebuildInstallFreeIpaAction.class,
            RebuildRestoreFreeIpaAction.class,
            RebuildRebootAction.class,
            RebuildRebootWaitUntilAvailableAction.class,
            RebuildCleanupFreeIpaAfterRestoreAction.class,
            RebuildPostInstallAction.class,
            RebuildValidateHealthAction.class,
            RebuildUpdateKerberosNameServersConfigAction.class,
            RebuildUpdateEnvironmentStackConfigAction.class,
            RebuildFinishedAction.class,
            RebuildFailedAction.class,
            ValidateBackupHandler.class,
            FreeIpaRestoreHandler.class,
            FreeIpaCleanupAfterRestoreHandler.class,
            RebuildValidateHealthHandler.class,
            DownscaleStackCollectResourcesHandler.class,
            DownscaleStackHandler.class,
            FreeipaUpscaleStackHandler.class,
            ClusterProxyRegistrationHandler.class,
            CollectMetadataHandler.class,
            BootstrapMachineHandler.class,
            OrchestratorConfigHandler.class,
            ValidateCloudStorageHandler.class,
            InstallFreeIpaServicesHandler.class,
            PostInstallFreeIpaHandler.class,
            FreeIpaRebuildFlowConfig.class,
            FlowIntegrationTestConfig.class,
            CloudPlatformInitializer.class,
            ResourceToCloudResourceConverter.class,
            ResourceAttributeUtil.class,
            RebootInstanceHandler.class,
            HealthCheckHandler.class,
            RebuildValidateHealthHandler.class,
            WebApplicationExceptionMessageExtractor.class
    })
    static class Config {

    }
}
