package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.handler.CheckImageHandler;
import com.sequenceiq.cloudbreak.cloud.handler.CollectMetadataHandler;
import com.sequenceiq.cloudbreak.cloud.handler.CreateCredentialHandler;
import com.sequenceiq.cloudbreak.cloud.handler.GetTlsInfoHandler;
import com.sequenceiq.cloudbreak.cloud.handler.PrepareImageHandler;
import com.sequenceiq.cloudbreak.cloud.handler.ProvisionSetupHandler;
import com.sequenceiq.cloudbreak.cloud.handler.ProvisionValidationHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;
import com.sequenceiq.common.api.type.ResourceType;
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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler.LoadBalancerUpdateHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.BootstrapMachineHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.ClusterProxyUpdateRegistrationHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.InstallFreeIpaServicesHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.OrchestratorConfigHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.PostInstallFreeIpaHandler;
import com.sequenceiq.freeipa.flow.freeipa.provision.handler.ValidateCloudStorageHandler;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.FreeipaUpscaleStackHandler;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.UpscaleCreateUserdataSecretsHandler;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.UpscaleUpdateUserdataSecretsHandler;
import com.sequenceiq.freeipa.flow.freeipa.upscale.handler.ValidateInstancesHealthHandler;
import com.sequenceiq.freeipa.flow.stack.provision.action.CheckImageAction;
import com.sequenceiq.freeipa.flow.stack.provision.action.StackProvisionService;
import com.sequenceiq.freeipa.flow.stack.provision.handler.ClusterProxyRegistrationHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.CreateUserDataHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.ImageFallbackHandler;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.TlsSetupService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaCloudStorageValidationService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaInstallService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaOrchestrationConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaPostInstallService;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerUpdateService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.InstanceGroupAttributeAndStackTemplateUpdater;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceValidationService;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "freeipa.delayed.scale-sec=1",
})
class FreeIpaUpscaleFlowIntegrationTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "opId";

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
    private ImageService imageService;

    @MockBean
    private ImageFallbackService imageFallbackService;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private MetadataSetupService metadataSetupService;

    @MockBean
    private TlsSetupService tlsSetupService;

    @MockBean
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @MockBean
    private PollTaskFactory pollTaskFactory;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private InstanceGroupService instanceGroupService;

    @MockBean
    private OperationService operationService;

    @MockBean
    private KerberosConfigUpdateService kerberosConfigUpdateService;

    @MockBean
    private EnvironmentEndpoint environmentEndpoint;

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory crnGeneratorFactory;

    @MockBean
    private NodeConfig nodeConfig;

    @MockBean
    private EncryptionKeyService encryptionKeyService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private InstanceGroupAttributeAndStackTemplateUpdater instanceGroupAttributeAndStackTemplateUpdater;

    @MockBean
    private FreeIpaLoadBalancerUpdateService loadBalancerUpdateService;

    @MockBean
    private FreeIpaLoadBalancerService loadBalancerService;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @MockBean
    private StackStatusFinalizer stackStatusFinalizer;

    private ResourceConnector resourceConnector = mock(ResourceConnector.class);

    private Stack stack;

    @BeforeEach
    public void setup() throws QuotaExceededException {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId("asfd");
        stack.setStackStatus(new StackStatus(stack, "test", DetailedStackStatus.PROVISION_REQUESTED));
        InstanceGroup masterIg = new InstanceGroup();
        masterIg.setGroupName("master");
        masterIg.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData im0 = new InstanceMetaData();
        im0.setInstanceStatus(InstanceStatus.CREATED);
        im0.setPrivateId(0L);
        im0.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterIg.setInstanceMetaData(new HashSet<>(List.of(im0)));
        stack.setInstanceGroups(new HashSet<>(List.of(masterIg)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(mock(Authenticator.class));
        Setup setup = mock(Setup.class);
        when(cloudConnector.setup()).thenReturn(setup);
        when(setup.checkImageStatus(any(), any(), any())).thenReturn(new ImageStatusResult(ImageStatus.CREATE_FINISHED, 100));
        CredentialConnector credentialConnector = mock(CredentialConnector.class);
        when(cloudConnector.credentials()).thenReturn(credentialConnector);
        when(credentialConnector.create(any())).thenReturn(mock(CloudCredentialStatus.class));
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(imageService.getByStack(stack)).thenReturn(new ImageEntity());
        when(stackToCloudStackConverter.convert(stack)).thenReturn(mock(CloudStack.class));
        PollTask pollTask = mock(PollTask.class);
        when(pollTask.completed(any())).thenReturn(Boolean.TRUE);
        when(pollTaskFactory.newPollResourcesStateTask(any(), any(), anyBoolean())).thenReturn(pollTask);
        when(cloudConnector.metadata()).thenReturn(mock(MetadataCollector.class));
        when(resourceConnector.getTlsInfo(any(), any())).thenReturn(new TlsInfo(false));
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getType()).thenReturn(ResourceType.AZURE_INSTANCE);
        when(resourceConnector.upscale(any(), any(), anyList(), any()))
                .thenReturn(List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(im0));
        when(crnGeneratorFactory.iam()).thenReturn(mock(RegionAwareInternalCrnGenerator.class));
        when(cachedEnvironmentClientService.getByCrn(any())).thenReturn(new DetailedEnvironmentResponse());
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(any(), anyList(), anyList())).thenReturn(stack);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testSuccessfulUpscale() {
        testFlow();
        verify(imageFallbackService).determineFallbackImageIfPermitted(any());
        verify(operationService).completeOperation(eq(stack.getAccountId()), eq(OPERATION_ID), any(), any());
        verify(instanceGroupAttributeAndStackTemplateUpdater).updateInstanceGroupAttributesAndTemplateIfDefaultDifferent(any(), eq(stack));
    }

    @Test
    public void testImageFallbackMoreThanOnce() throws Exception {
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure"));
        when(imageFallbackService.determineFallbackImageIfPermitted(any())).thenReturn(Optional.of("fallbackImageId"));
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPSCALE_FAILED, "Image fallback started second time!");
        verify(imageFallbackService).performImageFallback(any(), eq(stack));
        verifyNoInteractions(tlsSetupService, metadataSetupService);
        verify(operationService).failOperation(eq(stack.getAccountId()), eq(OPERATION_ID),
                eq("Upscale failed during [Image fallback]. Reason: Image fallback started second time!"), any(), any());
    }

    @Test
    public void testImageFallback() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getType()).thenReturn(ResourceType.AZURE_INSTANCE);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure"))
                .thenReturn(List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(imageFallbackService.determineFallbackImageIfPermitted(any())).thenReturn(Optional.of("fallbackImageId"));
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
        verify(imageFallbackService).performImageFallback(any(), eq(stack));
        verify(operationService).completeOperation(eq(stack.getAccountId()), eq(OPERATION_ID), any(), any());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPSCALE_IN_PROGRESS, "MP image failure");
    }

    @Test
    public void testImageFallbackNotPermittedWithoutFallbackImage() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getType()).thenReturn(ResourceType.AZURE_INSTANCE);
        when(resourceConnector.upscale(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure"))
                .thenReturn(List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(imageFallbackService.determineFallbackImageIfPermitted(any())).thenReturn(Optional.empty());
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        verify(imageFallbackService, never()).performImageFallback(any(), eq(stack));
        ArgumentCaptor<Collection<FailureDetails>> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).failOperation(eq(stack.getAccountId()), eq(OPERATION_ID),
                eq("Upscale failed during [Adding instances]. Reason: MP image failure"), any(),
                failureCaptor.capture());
        assertEquals("MP image failure", failureCaptor.getValue().stream().toList().getFirst().getAdditionalDetails().get("statusReason"));
    }

    @Test
    public void testUpscaleFailedNodeCountChanged() throws Exception {
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(any(), anyList(), anyList())).thenAnswer(new Answer<Stack>() {
            @Override
            public Stack answer(InvocationOnMock invocation) throws Throwable {
                List<CloudInstance> argument = invocation.getArgument(1, List.class);
                for (CloudInstance cloudInstance : argument) {
                    InstanceMetaData im = new InstanceMetaData();
                    im.setPrivateId(1L);
                    im.setInstanceStatus(InstanceStatus.REQUESTED);
                    stack.getInstanceGroups().stream().findFirst().get().getInstanceMetaData().add(im);
                }
                return stack;
            }
        });

        when(stackUpdater.updateStackStatus((Stack) any(), any(), eq("Installing FreeIPA"))).thenThrow(new IllegalArgumentException("Upscale failed."));

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        assertEquals(2, stack.getInstanceGroups().stream().findFirst().get().getNodeCount());
        verify(environmentService).setFreeIpaNodeCount(stack.getEnvironmentCrn(), 2);

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
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPSCALE_COMPLETED, "Upscale complete");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UpscaleFlowEvent.UPSCALE_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new UpscaleEvent(selector, STACK_ID, new ArrayList<>(), 2, false, false, false, OPERATION_ID, null)));
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
    @Import({
            FreeIpaUpscaleActions.class,
            UpscaleFlowConfig.class,
            CheckImageAction.class,
            ClusterProxyRegistrationHandler.class,
            CreateUserDataHandler.class,
            ImageFallbackHandler.class,
            StackProvisionService.class,
            FlowIntegrationTestConfig.class,
            CloudPlatformInitializer.class,
            ProvisionValidationHandler.class,
            ImageConverter.class,
            ProvisionSetupHandler.class,
            PrepareImageHandler.class,
            CheckImageHandler.class,
            CreateCredentialHandler.class,
            FreeipaUpscaleStackHandler.class,
            CollectMetadataHandler.class,
            GetTlsInfoHandler.class,
            PrivateIdProvider.class,
            WebApplicationExceptionMessageExtractor.class,
            BootstrapMachineHandler.class,
            OrchestratorConfigHandler.class,
            ValidateCloudStorageHandler.class,
            InstallFreeIpaServicesHandler.class,
            ClusterProxyUpdateRegistrationHandler.class,
            PostInstallFreeIpaHandler.class,
            ValidateInstancesHealthHandler.class,
            UpscaleCreateUserdataSecretsHandler.class,
            UpscaleUpdateUserdataSecretsHandler.class,
            LoadBalancerUpdateHandler.class
    })
    static class Config {
        @MockBean
        private ClusterProxyService clusterProxyService;

        @MockBean
        private UserDataService userDataService;

        @MockBean
        private SecurityConfigService securityConfigService;

        @MockBean
        private ResourceNotifier resourceNotifier;

        @MockBean
        private FreeipaJobService jobService;

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
        private FreeIpaSafeInstanceHealthDetailsService freeIpaSafeInstanceHealthDetailsService;

        @MockBean
        private InstanceValidationService instanceValidationService;

        @MockBean
        private UserdataSecretsService userdataSecretsService;

        @MockBean
        private StackEncryptionService stackEncryptionService;

        @MockBean
        private ResourceRetriever resourceRetriever;

        @MockBean
        private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;
    }
}