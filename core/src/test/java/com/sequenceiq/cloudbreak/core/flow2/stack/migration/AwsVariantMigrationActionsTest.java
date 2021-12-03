package com.sequenceiq.cloudbreak.core.flow2.stack.migration;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_UPDATE_FAILED;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class AwsVariantMigrationActionsTest {

    private static final long STACK_ID = 1234L;

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
    private CloudbreakMetricService metricService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    private AwsVariantMigrationFlowContext context;

    @BeforeEach
    void setUp() {
        context = new AwsVariantMigrationFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    public void testChangeVariantWhenCFTemplateDeleted() throws Exception {
        DeleteCloudFormationResult payload = new DeleteCloudFormationResult(STACK_ID, true);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getChangeVariantAction()).doExecute(context, payload, variables);
        verify(stackUpdater).updateVariant(payload.getResourceId(), CloudConstants.AWS_NATIVE);
        verify(metricService).incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_SUCCESSFUL, stack);
    }

    @Test
    public void testChangeVariantWhenCFTemplateNotDeleted() throws Exception {
        DeleteCloudFormationResult payload = new DeleteCloudFormationResult(STACK_ID, false);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getChangeVariantAction()).doExecute(context, payload, variables);
        verify(stackUpdater, never()).updateVariant(payload.getResourceId(), CloudConstants.AWS_NATIVE);
    }

    @Test
    public void testMigrationFailed() throws Exception {
        String errorReason = "error reason";
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason));
        StackFailureContext stackFailureContext = new StackFailureContext(flowParameters, new StackView());
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(getMigrationFailedAction()).doExecute(stackFailureContext, payload, variables);
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_FAILED.name(), STACK_INFRASTRUCTURE_UPDATE_FAILED, errorReason);
        verify(metricService).incrementMetricCounter(MetricType.AWS_VARIANT_MIGRATION_FAILED, stackFailureContext.getStackView(), payload.getException());
    }

    private AbstractAwsVariantMigrationAction<DeleteCloudFormationResult> getChangeVariantAction() {
        AbstractAwsVariantMigrationAction<DeleteCloudFormationResult> action =
                (AbstractAwsVariantMigrationAction<DeleteCloudFormationResult>) underTest.changeVariant();
        initActionPrivateFields(action);
        return action;
    }

    private AbstractStackFailureAction<AwsVariantMigrationFlowState, AwsVariantMigrationEvent> getMigrationFailedAction() {
        AbstractStackFailureAction<AwsVariantMigrationFlowState, AwsVariantMigrationEvent> action =
                (AbstractStackFailureAction<AwsVariantMigrationFlowState, AwsVariantMigrationEvent>) underTest.migrationFailed();
        initActionPrivateFields(action);
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
