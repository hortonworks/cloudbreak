package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_AWS_VARIANT_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_AWS_VARIANT_MIGRATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_AWS_VARIANT_MIGRATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.migration.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.metrics.MetricType;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
public class AwsVariantMigrationActionsTest {

    private static final long STACK_ID = 1234L;

    private static final String ENV_CRN = "crn:env";

    @InjectMocks
    private AwsVariantMigrationActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private FreeIpaMetricService metricService;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private OperationService operationService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private EnvironmentService environmentService;

    private AwsVariantMigrationFlowContext context;

    @BeforeEach
    void setUp() {
        context = new AwsVariantMigrationFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    public void testChangeVariantWhenCFTemplateDeleted() throws Exception {
        DeleteCloudFormationResult payload = new DeleteCloudFormationResult(STACK_ID, true);
        Map<Object, Object> variables = new HashMap<>();
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        new AbstractActionTestSupport<>(getChangeVariantAction()).doExecute(context, payload, variables);
        verify(stackUpdater).updateVariant(payload.getResourceId(), CloudConstants.AWS_NATIVE);
        verify(environmentService).setFreeIpaPlatformVariant(ENV_CRN, CloudConstants.AWS_NATIVE);
        verify(eventSenderService).sendEventAndNotification(stack, flowParameters.getFlowTriggerUserCrn(), FREEIPA_AWS_VARIANT_MIGRATION_FINISHED);
    }

    @Test
    public void testChangeVariantWhenCFTemplateNotDeleted() throws Exception {
        DeleteCloudFormationResult payload = new DeleteCloudFormationResult(STACK_ID, false);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getChangeVariantAction()).doExecute(context, payload, variables);
        verify(stackUpdater, never()).updateVariant(payload.getResourceId(), CloudConstants.AWS_NATIVE);
        verify(environmentService, never()).setFreeIpaPlatformVariant(any(), any());
        verify(eventSenderService).sendEventAndNotification(stack, flowParameters.getFlowTriggerUserCrn(), FREEIPA_AWS_VARIANT_MIGRATION_FINISHED);
    }

    @Test
    public void testCreateResourcesSendsStartedNotification() throws Exception {
        AwsVariantMigrationTriggerEvent payload = new AwsVariantMigrationTriggerEvent("selector", STACK_ID, "master");
        Map<Object, Object> variables = new HashMap<>();

        AbstractAwsVariantMigrationAction<AwsVariantMigrationTriggerEvent> action =
                (AbstractAwsVariantMigrationAction<AwsVariantMigrationTriggerEvent>) underTest.createResources();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, flowParameters.getFlowTriggerUserCrn(), FREEIPA_AWS_VARIANT_MIGRATION_STARTED);
    }

    @Test
    public void testMigrationFailedUnderUpgradeChainUpdatesUpgradeFailedStatusAndSendsUpgradeFailedNotification() throws Exception {
        String errorReason = "error reason";
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason), ERROR);
        Map<Object, Object> variables = new HashMap<>();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        when(operationService.failOperation(any(), any(), any(), any(), any())).thenReturn(operation);
        String triggerUserCrn = "trigger-user-crn";
        when(flowParameters.getFlowTriggerUserCrn()).thenReturn(triggerUserCrn);

        new AbstractActionTestSupport<>(getMigrationFailedAction()).doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPGRADE_FAILED), anyString());
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(triggerUserCrn), eq(FREEIPA_AWS_VARIANT_MIGRATION_FAILED),
                eq(List.of(errorReason)));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(triggerUserCrn), eq(FREEIPA_UPGRADE_FAILED), any());
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), any(), eq(FREEIPA_MULTI_AZ_MIGRATION_FAILED), any());
        verify(metricService).incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_FAILED, context.getStack(), payload.getException());
    }

    @Test
    public void testMigrationFailedUnderMultiAzChainUpdatesMultiAzMigrationFailedStatusAndSendsMultiAzMigrationFailedNotification() throws Exception {
        String errorReason = "error reason";
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason), ERROR);
        Map<Object, Object> variables = new HashMap<>();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.MIGRATE_TO_MULTI_AZ);
        when(operationService.failOperation(any(), any(), any(), any(), any())).thenReturn(operation);
        String triggerUserCrn = "trigger-user-crn";
        when(flowParameters.getFlowTriggerUserCrn()).thenReturn(triggerUserCrn);

        new AbstractActionTestSupport<>(getMigrationFailedAction()).doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED), anyString());
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(triggerUserCrn), eq(FREEIPA_AWS_VARIANT_MIGRATION_FAILED),
                eq(List.of(errorReason)));
        verify(eventSenderService).sendEventAndNotification(eq(stack), eq(triggerUserCrn), eq(FREEIPA_MULTI_AZ_MIGRATION_FAILED),
                any());
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), any(), eq(FREEIPA_UPGRADE_FAILED), any());
        verify(metricService).incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_FAILED, context.getStack(), payload.getException());
    }

    private AbstractAwsVariantMigrationAction<DeleteCloudFormationResult> getChangeVariantAction() {
        AbstractAwsVariantMigrationAction<DeleteCloudFormationResult> action =
                (AbstractAwsVariantMigrationAction<DeleteCloudFormationResult>) underTest.changeVariant();
        initActionPrivateFields(action);
        return action;
    }

    private AbstractAwsVariantMigrationAction<StackFailureEvent> getMigrationFailedAction() {
        AbstractAwsVariantMigrationAction<StackFailureEvent> action = (AbstractAwsVariantMigrationAction<StackFailureEvent>) underTest.migrationFailed();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, null, stackUpdater, StackUpdater.class);
    }
}
