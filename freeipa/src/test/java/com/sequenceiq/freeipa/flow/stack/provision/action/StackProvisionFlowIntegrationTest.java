package com.sequenceiq.freeipa.flow.stack.provision.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CREATION_STARTED;
import static com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent.START_CREATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.handler.CheckImageHandler;
import com.sequenceiq.cloudbreak.cloud.handler.CollectMetadataHandler;
import com.sequenceiq.cloudbreak.cloud.handler.CreateCredentialHandler;
import com.sequenceiq.cloudbreak.cloud.handler.GetTlsInfoHandler;
import com.sequenceiq.cloudbreak.cloud.handler.LaunchStackHandler;
import com.sequenceiq.cloudbreak.cloud.handler.PrepareImageHandler;
import com.sequenceiq.cloudbreak.cloud.handler.ProvisionSetupHandler;
import com.sequenceiq.cloudbreak.cloud.handler.ProvisionValidationHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;
import com.sequenceiq.common.api.type.ResourceType;
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
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionFlowConfig;
import com.sequenceiq.freeipa.flow.stack.provision.handler.ClusterProxyRegistrationHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.CreateUserDataHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.GenerateEncryptionKeysHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.ImageFallbackHandler;
import com.sequenceiq.freeipa.flow.stack.provision.handler.UpdateUserdataSecretsHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.TlsSetupService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class StackProvisionFlowIntegrationTest {

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
    private NodeConfig nodeConfig;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @MockBean
    private EncryptionKeyService encryptionKeyService;

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

    private ResourceConnector resourceConnector = mock(ResourceConnector.class);

    private Stack stack;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setAccountId("asfd");
        stack.setStackStatus(new StackStatus(stack, "test", DetailedStackStatus.PROVISION_REQUESTED));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(imageFallbackService.determineFallbackImageIfPermitted(any())).thenReturn(Optional.of("fallbackImageId"));
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
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(new InstanceMetaData()));
        when(cachedEnvironmentClientService.getByCrn(any())).thenReturn(new DetailedEnvironmentResponse());
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testSuccessfulProvision() {
        testFlow();
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_STARTED));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FINISHED));
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FAILED), any());
    }

    @Test
    public void testImageFallbackMoreThanOnce() throws Exception {
        when(resourceConnector.launch(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure"));
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.PROVISION_FAILED, "Image fallback started second time!");
        verify(imageFallbackService).performImageFallback(any(), eq(stack));
        verifyNoInteractions(tlsSetupService, metadataSetupService);
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_STARTED));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FAILED), any());
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FINISHED));
    }

    @Test
    public void testImageFallback() throws Exception {
        when(resourceConnector.launch(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure")).thenReturn(List.of());
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
        verify(imageFallbackService).performImageFallback(any(), eq(stack));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_STARTED));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FINISHED));
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FAILED), any());
    }

    @Test
    public void testImageFallbackNotPermittedWithoutFallbackImage() throws Exception {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getType()).thenReturn(ResourceType.AZURE_INSTANCE);
        when(resourceConnector.launch(any(), any(), any(), any())).thenThrow(new CloudImageException("MP image failure"))
                .thenReturn(List.of(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED)));
        when(imageFallbackService.determineFallbackImageIfPermitted(any())).thenReturn(Optional.empty());
        stack.setCloudPlatform(CloudPlatform.AZURE.name());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        verify(imageFallbackService, never()).performImageFallback(any(), eq(stack));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_STARTED));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FAILED), any());
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), eq(USER_CRN), eq(FREEIPA_CREATION_FINISHED));
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
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.STACK_PROVISIONED, "Stack provisioned.");
    }

    private FlowIdentifier triggerFlow() {
        String selector = START_CREATION_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new StackEvent(selector, STACK_ID)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            StackProvisionActions.class,
            StackProvisionFlowConfig.class,
            CheckImageAction.class,
            ClusterProxyRegistrationHandler.class,
            GenerateEncryptionKeysHandler.class,
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
            LaunchStackHandler.class,
            CollectMetadataHandler.class,
            GetTlsInfoHandler.class,
            UpdateUserdataSecretsHandler.class
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
        private UserdataSecretsService userdataSecretsService;

        @MockBean
        private StackEncryptionService stackEncryptionService;

        @MockBean
        private ResourceRetriever resourceRetriever;

        @MockBean
        private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;
    }
}