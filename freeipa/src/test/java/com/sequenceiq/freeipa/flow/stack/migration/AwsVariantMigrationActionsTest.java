package com.sequenceiq.freeipa.flow.stack.migration;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.metrics.MetricType;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

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
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason), ERROR);
        Map<Object, Object> variables = new HashMap<>();
        when(runningFlows.get(any())).thenReturn(mock(Flow.class));
        new AbstractActionTestSupport<>(getMigrationFailedAction()).doExecute(context, payload, variables);
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
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
