package com.sequenceiq.freeipa.flow.freeipa.rebuild;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.UPDATED;
import static com.sequenceiq.common.api.type.Tunnel.CLUSTER_PROXY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REBUILD_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
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
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.handler.CollectMetadataHandler;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackCollectResourcesHandler;
import com.sequenceiq.cloudbreak.cloud.handler.DownscaleStackHandler;
import com.sequenceiq.cloudbreak.cloud.handler.RebootInstanceHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientCallable;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaValidationProperties;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler.LoadBalancerUpdateHandler;
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
import com.sequenceiq.freeipa.flow.freeipa.rebuild.action.RebuildUpdateLoadBalancerAction;
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
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.FreeipaUpscaleStackHandler;
import com.sequenceiq.freeipa.flow.stack.HealthCheckHandler;
import com.sequenceiq.freeipa.flow.stack.StackContext;
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
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerUpdateService;
import com.sequenceiq.freeipa.service.operation.OperationService;
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

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String OPERATION_ID = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = "accId";

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
    private FreeIpaLoadBalancerUpdateService freeIpaLoadBalancerUpdateService;

    @MockBean
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @MockBean
    private EnvironmentEndpoint environmentEndpoint;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @MockBean
    private OperationService operationService;

    @Inject
    private FreeIpaService freeIpaService;

    private Stack stack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private CloudStack cloudStack;

    private InstanceMetaData instanceMetaData;

    @Mock
    private MetadataCollector metadataCollector;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private InstanceConnector instanceConnector;

    @Mock
    private FreeIpaClient freeIpaClient;

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

    @BeforeEach
    public void setup() throws FreeIpaClientException, QuotaExceededException, CloudbreakOrchestratorException {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(CLUSTER_PROXY);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        InstanceGroup ig = new InstanceGroup();
        instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetaData.setDiscoveryFQDN("ipaserver0.example.com");
        ig.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(ig));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenReturn(ac);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(resourceConnector.upscale(any(), any(), any(), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)))).thenAnswer(inv -> {
            instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            return List.of();
        });
        PollTask<ResourcesStatePollerResult> pollTask = mock(PollTask.class);
        when(statusCheckFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(pollTask.completed(any())).thenReturn(Boolean.TRUE);
        when(cloudConnector.metadata()).thenReturn(metadataCollector);
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(instanceMetaData));
        when(gatewayConfig.getHostname()).thenReturn("ipaserver0.example.com");
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(stackToCloudStackConverter.buildInstance(eq(stack), any(InstanceMetaData.class), eq(ig), any(), any(), any()))
                .thenReturn(mock(CloudInstance.class));
        when(cloudConnector.instances()).thenReturn(instanceConnector);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        lenient().doAnswer(invocation -> invocation.getArgument(0, FreeIpaClientCallable.class).run())
                .when(retryService).retryWhenRetryableWithValue(any(FreeIpaClientCallable.class));
        when(instanceMetaDataToCloudInstanceConverter.convert(instanceMetaData)).thenReturn(cloudInstance);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("example.com");
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa);
        NodeHealthDetails healthDetails = new NodeHealthDetails();
        healthDetails.setStatus(InstanceStatus.CREATED);
        when(healthDetailsService.getInstanceHealthDetails(stack, instanceMetaData)).thenReturn(healthDetails);
        when(regionAwareInternalCrnGeneratorFactory.iam())
                .thenReturn(RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator(Crn.Service.IAM, "cdp", "us-west-1"));
        when(stackToCloudStackConverter.convert(stack)).thenReturn(cloudStack);
        when(cloudInstance.getInstanceId()).thenReturn("instance-id");
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testRebuildWhenSuccessful() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        NodeHealthDetails healthDetails = new NodeHealthDetails();
        healthDetails.setStatus(InstanceStatus.CREATED);
        when(healthDetailsService.getInstanceHealthDetails(stack, instanceMetaData)).thenReturn(healthDetails);

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Installing FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Restoring FreeIPA from backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Cleanup FreeIPA after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA Post Installation");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validate FreeIPA health");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating kerberos nameserver config");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating FreeIPA load balancer");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating clusters' configuration");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.AVAILABLE, "Rebuild finished");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(2)).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verify(freeIpaInstallService).installFreeIpa(STACK_ID);
        verify(instanceConnector).reboot(ac, List.of(), List.of(cloudInstance));
        verify(freeIpaServiceStartService).pollFreeIpaHealth(stack);
        verify(stackStatusCheckerJob).syncAStack(stack, true);
        verify(cleanupService).removeServers(STACK_ID, Set.of("ipaserver1.example.com"));
        verify(cleanupService).removeDnsEntries(STACK_ID, Set.of("ipaserver1.example.com"), Set.of(), "example.com", ENVIRONMENT_CRN);
        verify(freeIpaPostInstallService).postInstallFreeIpa(STACK_ID, false);
        verify(kerberosConfigUpdateService).updateNameservers(STACK_ID);
        verify(environmentEndpoint).updateConfigsInEnvironmentByCrn(ENVIRONMENT_CRN);
        verify(operationService).completeOperation(ACCOUNT_ID, OPERATION_ID, List.of(new SuccessDetails(ENVIRONMENT_CRN)), List.of());
    }

    @Test
    public void testHealthValidateFails() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        NodeHealthDetails healthDetails = new NodeHealthDetails();
        healthDetails.setStatus(InstanceStatus.FAILED);
        when(healthDetailsService.getInstanceHealthDetails(stack, instanceMetaData)).thenReturn(healthDetails);

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Installing FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Restoring FreeIPA from backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Cleanup FreeIPA after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA Post Installation");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validate FreeIPA health");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_FAILED,
                "Failed to rebuild FreeIPA: Instance(s) healthcheck failed: " +
                        "[NodeHealthDetails{issues=[], status=FAILED, name='null', instanceId='null', healthChecks='[]'}]");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(2)).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verify(freeIpaInstallService).installFreeIpa(STACK_ID);
        verify(instanceConnector).reboot(ac, List.of(), List.of(cloudInstance));
        verify(freeIpaServiceStartService).pollFreeIpaHealth(stack);
        verify(stackStatusCheckerJob).syncAStack(stack, true);
        verify(cleanupService).removeServers(STACK_ID, Set.of("ipaserver1.example.com"));
        verify(cleanupService).removeDnsEntries(STACK_ID, Set.of("ipaserver1.example.com"), Set.of(), "example.com", ENVIRONMENT_CRN);
        verify(freeIpaPostInstallService).postInstallFreeIpa(STACK_ID, false);
        verifyNoInteractions(kerberosConfigUpdateService);
        verifyNoInteractions(environmentEndpoint);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID,
                "Failed to rebuild FreeIPA: Instance(s) healthcheck failed: " +
                        "[NodeHealthDetails{issues=[], status=FAILED, name='null', instanceId='null', healthChecks='[]'}]");
    }

    @Test
    public void testPostInstallFails() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        doThrow(new Exception("postinstall failure")).when(freeIpaPostInstallService).postInstallFreeIpa(STACK_ID, false);

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Installing FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Restoring FreeIPA from backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Cleanup FreeIPA after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA Post Installation");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_FAILED, "Failed to rebuild FreeIPA: postinstall failure");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(2)).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verify(freeIpaInstallService).installFreeIpa(STACK_ID);
        verify(instanceConnector).reboot(ac, List.of(), List.of(cloudInstance));
        verify(freeIpaServiceStartService).pollFreeIpaHealth(stack);
        verify(stackStatusCheckerJob).syncAStack(stack, true);
        verify(cleanupService).removeServers(STACK_ID, Set.of("ipaserver1.example.com"));
        verify(cleanupService).removeDnsEntries(STACK_ID, Set.of("ipaserver1.example.com"), Set.of(), "example.com", ENVIRONMENT_CRN);
        verifyNoInteractions(kerberosConfigUpdateService);
        verifyNoInteractions(environmentEndpoint);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID,
                "Failed to rebuild FreeIPA: postinstall failure");
    }

    @Test
    public void testCleanupFails() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        doThrow(new FreeIpaClientException("cleanup failure")).when(cleanupService).removeServers(STACK_ID, Set.of("ipaserver1.example.com"));

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Installing FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Restoring FreeIPA from backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Rebooting FreeIPA instance after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Waiting for FreeIPA to be available");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Cleanup FreeIPA after restore");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_FAILED, "Failed to rebuild FreeIPA: cleanup failure");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(2)).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verify(freeIpaInstallService).installFreeIpa(STACK_ID);
        verify(instanceConnector).reboot(ac, List.of(), List.of(cloudInstance));
        verify(freeIpaServiceStartService).pollFreeIpaHealth(stack);
        verify(stackStatusCheckerJob).syncAStack(stack, true);
        verifyNoInteractions(freeIpaPostInstallService);
        verifyNoInteractions(kerberosConfigUpdateService);
        verifyNoInteractions(environmentEndpoint);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID,
                "Failed to rebuild FreeIPA: cleanup failure");
    }

    @Test
    public void testRestoreFails() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        doNothing().doThrow(new CloudbreakOrchestratorFailedException("restore failed"))
                .when(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Installing FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_FAILED, "Failed to rebuild FreeIPA: restore failed");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(2)).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verify(freeIpaInstallService).installFreeIpa(STACK_ID);
        verifyNoInteractions(freeIpaPostInstallService, kerberosConfigUpdateService, environmentEndpoint, cleanupService, instanceConnector,
                freeIpaServiceStartService, stackStatusCheckerJob);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID,
                "Failed to rebuild FreeIPA: restore failed");
    }

    @Test
    public void testBackupValidationFails() throws Exception {
        List<CloudResource> cloudResources = List.of(mock(CloudResource.class));
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(cloudResources);
        ArgumentCaptor<List<CloudInstance>> instancesCaptor = ArgumentCaptor.forClass(List.class);
        Stack updatedStack = new Stack();
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(eq(stack), instancesCaptor.capture(), eq(List.of()))).thenReturn(updatedStack);
        CloudStack updatedCloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(updatedStack)).thenReturn(updatedCloudStack);
        CloudVmMetaDataStatus metaDataStatus = new CloudVmMetaDataStatus(new CloudVmInstanceStatus(cloudInstance,
                com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED), new CloudInstanceMetaData("1.2.3.4", "1.2.3.5"));
        when(metadataCollector.collect(eq(ac), eq(cloudResources), eq(List.of()), eq(List.of()))).thenReturn(List.of(metaDataStatus));
        IpaServer ipaServerCurrent = new IpaServer();
        ipaServerCurrent.setCn("ipaserver0.example.com");
        IpaServer ipaServerOldReplica = new IpaServer();
        ipaServerOldReplica.setCn("ipaserver1.example.com");
        when(freeIpaClient.findAllServers()).thenReturn(Set.of(ipaServerCurrent, ipaServerOldReplica));
        doThrow(new CloudbreakOrchestratorFailedException("backup dl and validate failed"))
                .when(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        testFlow();

        InOrder stackStatusVerify = inOrder(stackUpdater);
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "FreeIPA rebuild requested");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Updating metadata for deletion request");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Collecting resources");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Decommissioning instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Finished removing instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Create new instance");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating new instances");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Extending metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Saving metadata");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Setting up TLS");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Update cluster proxy registration before bootstrap");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Bootstrapping machines");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Configuring the orchestrator");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Validating cloud storage");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, REBUILD_IN_PROGRESS, "Downloading and validating backup");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REBUILD_VALIDATION_FAILED,
                "Failed to rebuild FreeIPA: backup dl and validate failed");
        stackStatusVerify.verifyNoMoreInteractions();

        verify(terminationService).requestDeletion(STACK_ID, null);
        verify(resourceConnector).collectResourcesToRemove(eq(ac), eq(cloudStack), eq(cloudResources), eq(List.of(cloudInstance)));
        verify(resourceConnector).downscale(ac, cloudStack, cloudResources, List.of(cloudInstance), List.of());
        verify(terminationService).terminateMetaDataInstances(stack, null);
        ArgumentCaptor<InstanceMetaData> imCaptor = ArgumentCaptor.forClass(InstanceMetaData.class);
        verify(instanceMetaDataService).save(imCaptor.capture());
        InstanceMetaData imCaptured = imCaptor.getValue();
        assertEquals(instanceMetaData, imCaptured);
        List<CloudInstance> newCloudInstances = instancesCaptor.getValue();
        assertEquals(1, newCloudInstances.size());
        verify(resourceConnector).upscale(eq(ac), eq(updatedCloudStack), eq(cloudResources), eq(new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L)));
        ArgumentCaptor<StackContext> stackContextCaptor = ArgumentCaptor.forClass(StackContext.class);
        ArgumentCaptor<UpscaleStackResult> upscaleStackResultCaptor = ArgumentCaptor.forClass(UpscaleStackResult.class);
        verify(instanceValidationService).finishAddInstances(stackContextCaptor.capture(), upscaleStackResultCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        UpscaleStackResult upscaleStackResult = upscaleStackResultCaptor.getValue();
        assertEquals(UPDATED, upscaleStackResult.getResourceStatus());
        assertEquals(STACK_ID, upscaleStackResult.getResourceId());
        assertTrue(upscaleStackResult.getResults().isEmpty());
        verify(metadataSetupService).saveInstanceMetaData(eq(stack), eq(List.of(metaDataStatus)), eq(CREATED));
        verify(stackProvisionService).setupTls(stackContextCaptor.capture());
        assertEquals(stack, stackContextCaptor.getValue().getStack());
        verify(clusterProxyService).registerFreeIpa(STACK_ID);
        verify(bootstrapService).bootstrap(STACK_ID);
        verify(orchestrationConfigService).configureOrchestrator(STACK_ID);
        verify(storageValidationService).validate(stack);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParamsCaptor.capture());
        OrchestratorStateParams orchestratorStateParams = orchestratorStateParamsCaptor.getValue();
        assertEquals(gatewayConfig, orchestratorStateParams.getPrimaryGatewayConfig());
        verifyNoInteractions(freeIpaPostInstallService, kerberosConfigUpdateService, environmentEndpoint, cleanupService, instanceConnector,
                freeIpaServiceStartService, stackStatusCheckerJob, freeIpaInstallService);
        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID,
                "Failed to rebuild FreeIPA: backup dl and validate failed");
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
        RebuildEvent rebuildEvent = new RebuildEvent(STACK_ID, "", "", "", OPERATION_ID);
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
            RebuildUpdateLoadBalancerAction.class,
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
            LoadBalancerUpdateHandler.class,
            WebApplicationExceptionMessageExtractor.class,
            FreeIpaFailedFlowAnalyzer.class,
            FreeIpaValidationProperties.class
    })
    static class Config {

    }
}
