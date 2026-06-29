package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_AWS_VARIANT_MIGRATION_FINISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsTerminateService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformInitializer;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.stack.migration.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.migration.handler.AwsMigrationUtil;
import com.sequenceiq.freeipa.flow.stack.migration.handler.CreateResourcesHandler;
import com.sequenceiq.freeipa.flow.stack.migration.handler.DeleteCloudFormationHandler;
import com.sequenceiq.freeipa.flow.stack.migration.handler.service.ResourceRecreator;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.AutoSyncConfig;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class AwsVariantMigrationFlowIntegrationTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String STACK_CRN = "crn:cdp:freeipa:us-west-1:" + ACCOUNT_ID + ":freeipa:" + UUID.randomUUID();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockitoBean
    private StackService stackService;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockitoBean
    private CredentialToCloudCredentialConverter credentialConverter;

    @MockitoBean
    private CredentialService credentialService;

    @MockitoBean
    private OperationService operationService;

    @MockitoBean
    private EventSenderService eventSenderService;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private NodeValidator nodeValidator;

    @MockitoBean
    private NodeConfig nodeConfig;

    @MockitoBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockitoBean
    private FlowCancelService flowCancelService;

    @MockitoBean
    private FlowUsageSender flowUsageSender;

    @MockitoBean
    private StackStatusFinalizer stackStatusFinalizer;

    @MockitoBean
    private AwsAuthenticator awsAuthenticator;

    @MockitoBean
    private AwsContextBuilder awsContextBuilder;

    @MockitoBean
    private AwsTerminateService awsTerminateService;

    @MockitoBean
    private AwsMigrationUtil awsMigrationUtil;

    @MockitoBean
    private ResourceRetriever resourceRetriever;

    @MockitoBean
    private ResourceNotifier resourceNotifier;

    @MockitoBean
    private ResourceRecreator resourceRecreator;

    @MockitoBean
    private EnvironmentService environmentService;

    @BeforeEach
    void setup() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("test-stack");
        stack.setRegion("us-east-1");
        stack.setAvailabilityZone("us-east-1a");
        stack.setCloudPlatform("AWS");
        stack.setPlatformvariant("AWS");
        stack.setResourceCrn(STACK_CRN);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setAccountId(ACCOUNT_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(credentialService.getCredentialByEnvCrn(anyString())).thenReturn(mock(Credential.class));
        when(credentialConverter.convert(any())).thenReturn(mock(CloudCredential.class));
        CloudStack cloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(stack)).thenReturn(cloudStack);
        when(awsAuthenticator.authenticate(any(), any())).thenReturn(null);
        when(awsContextBuilder.contextInit(any(), any(), any(), anyBoolean())).thenReturn(mock(AwsContext.class));
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    void testSuccessfulMigration() {
        when(resourceRetriever.findByStatusAndTypeAndStack(eq(CommonStatus.CREATED), eq(ResourceType.CLOUDFORMATION_STACK), eq(STACK_ID)))
                .thenReturn(Optional.empty());

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
        verify(stackUpdater).updateVariant(STACK_ID, "AWS_NATIVE");
        verify(eventSenderService).sendEventAndNotification(any(Stack.class), anyString(), eq(FREEIPA_AWS_VARIANT_MIGRATION_FINISHED));
    }

    @Test
    void testDeleteCloudFormationFailureRealErrorMessagePropagated() {
        String cfError = "Cannot delete stack: there is a dependent object";
        when(resourceRetriever.findByStatusAndTypeAndStack(eq(CommonStatus.CREATED), eq(ResourceType.CLOUDFORMATION_STACK), anyLong()))
                .thenThrow(new RuntimeException(cfError));
        Operation upgradeOperation = new Operation();
        upgradeOperation.setOperationType(OperationType.UPGRADE);
        when(operationService.failOperation(eq(ACCOUNT_ID), any(), anyString(), any(), any())).thenReturn(upgradeOperation);

        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();

        ArgumentCaptor<String> statusReasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(stackUpdater).updateStackStatus(any(Stack.class), eq(DetailedStackStatus.UPGRADE_FAILED), statusReasonCaptor.capture());
        assertThat(statusReasonCaptor.getValue()).contains(cfError);

        ArgumentCaptor<String> operationMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(operationService).failOperation(eq(ACCOUNT_ID), any(), operationMessageCaptor.capture(), any(), any());
        assertThat(operationMessageCaptor.getValue()).contains(cfError);
    }

    private FlowIdentifier triggerFlow() {
        String selector = AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector, new AwsVariantMigrationTriggerEvent(selector, STACK_ID, "master")));
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
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            FlowIntegrationTestConfig.class,
            FlowEventCommonListener.class,
            FlowMetricSender.class,
            CommonMetricService.class,
            AwsVariantMigrationFlowConfig.class,
            AwsVariantMigrationActions.class,
            CreateResourcesHandler.class,
            DeleteCloudFormationHandler.class,
            CloudPlatformInitializer.class,
            FreeipaJobService.class,
            AutoSyncConfig.class,
            StatusCheckerConfig.class,
            StatusCheckerJobService.class
    })
    static class Config {
    }
}
