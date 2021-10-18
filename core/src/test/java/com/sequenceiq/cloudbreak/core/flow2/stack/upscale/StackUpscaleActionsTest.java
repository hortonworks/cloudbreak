package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class StackUpscaleActionsTest {

    private static final String INSTANCE_GROUP_NAME = "worker";

    private static final Integer ADJUSTMENT = 3;

    private static final Integer ADJUSTMENT_ZERO = 0;

    private static final String SELECTOR = "selector";

    private static final Long STACK_ID = 123L;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackScalabilityCondition stackScalabilityCondition;

    @Mock
    private StackUpscaleService stackUpscaleService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private StackUpscaleActions underTest;

    private StackScalingFlowContext context;

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

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @Mock
    private CloudResourceStatus cloudResourceStatus;

    @BeforeEach
    void setUp() {
        context = new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME, Set.of(), ADJUSTMENT,
                false, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT.longValue()));
    }

    private AbstractStackUpscaleAction<StackScaleTriggerEvent> getPrevalidateAction() {
        AbstractStackUpscaleAction<StackScaleTriggerEvent> action = (AbstractStackUpscaleAction<StackScaleTriggerEvent>) underTest.prevalidate();
        initActionPrivateFields(action);
        return action;
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    // Note: this implicitly tests getPrevalidateAction().createRequest() as well.
    @Test
    void prevalidateTestDoExecuteWhenScalingNeededAndAllowed() throws Exception {
        when(cloudContext.getId()).thenReturn(STACK_ID);
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT.longValue());
        StackScaleTriggerEvent payload = new StackScaleTriggerEvent(SELECTOR, STACK_ID, INSTANCE_GROUP_NAME, ADJUSTMENT,
                adjustmentTypeWithThreshold);

        when(stackScalabilityCondition.isScalable(stack, INSTANCE_GROUP_NAME)).thenReturn(true);

        Stack updatedStack = mock(Stack.class);
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, 3, INSTANCE_GROUP_NAME, false, context.getHostNames(), false,
                context.getStackNetworkScaleDetails()))
                .thenReturn(updatedStack);
        CloudStack convertedCloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(updatedStack)).thenReturn(convertedCloudStack);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, Map.of());

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, ADJUSTMENT, INSTANCE_GROUP_NAME, adjustmentTypeWithThreshold);
        verify(stackUpscaleService).startAddInstances(stack, ADJUSTMENT, INSTANCE_GROUP_NAME);

        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        verify(eventBus).notify("UPSCALESTACKVALIDATIONREQUEST", event);

        Object responsePayload = payloadArgumentCaptor.getValue();
        assertThat(responsePayload).isInstanceOf(UpscaleStackValidationRequest.class);

        UpscaleStackValidationRequest<UpscaleStackValidationResult> upscaleStackValidationRequest =
                (UpscaleStackValidationRequest<UpscaleStackValidationResult>) responsePayload;
        assertThat(upscaleStackValidationRequest.getResourceId()).isEqualTo(STACK_ID);
        assertThat(upscaleStackValidationRequest.getCloudContext()).isSameAs(cloudContext);
        assertThat(upscaleStackValidationRequest.getCloudStack()).isSameAs(convertedCloudStack);
        assertThat(upscaleStackValidationRequest.getCloudCredential()).isSameAs(cloudCredential);
    }

    @Test
    void prevalidateTestDoExecuteWhenScalingNeededAndNotAllowed() throws Exception {
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT.longValue());
        StackScaleTriggerEvent payload = new StackScaleTriggerEvent(SELECTOR, STACK_ID, INSTANCE_GROUP_NAME, ADJUSTMENT,
                adjustmentTypeWithThreshold);

        when(stackScalabilityCondition.isScalable(stack, INSTANCE_GROUP_NAME)).thenReturn(false);

        List<CloudResourceStatus> resourceStatuses = List.of(cloudResourceStatus);
        when(resourceService.getAllAsCloudResourceStatus(STACK_ID)).thenReturn(resourceStatuses);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, Map.of());

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, ADJUSTMENT, INSTANCE_GROUP_NAME, adjustmentTypeWithThreshold);
        verifyEventForUpscaleStackResult(resourceStatuses);
    }

    private void verifyEventForUpscaleStackResult(List<CloudResourceStatus> resourceStatuses) {
        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        verify(eventBus).notify("UPSCALESTACKRESULT", event);

        Object responsePayload = payloadArgumentCaptor.getValue();
        assertThat(responsePayload).isInstanceOf(UpscaleStackResult.class);

        UpscaleStackResult upscaleStackResult = (UpscaleStackResult) responsePayload;
        assertThat(upscaleStackResult.getResourceId()).isEqualTo(STACK_ID);
        assertThat(upscaleStackResult.getResourceStatus()).isEqualTo(ResourceStatus.CREATED);
        assertThat(upscaleStackResult.getResults()).isEqualTo(resourceStatuses);
        assertThat(upscaleStackResult.getStatus()).isEqualTo(EventStatus.OK);
        assertThat(upscaleStackResult.getStatusReason()).isNull();
        assertThat(upscaleStackResult.getErrorDetails()).isNull();
    }

    @Test
    void prevalidateTestDoExecuteWhenScalingNotNeeded() throws Exception {
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT_ZERO.longValue());
        context = new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, INSTANCE_GROUP_NAME, Set.of(), ADJUSTMENT_ZERO,
                false, adjustmentTypeWithThreshold);
        StackScaleTriggerEvent payload = new StackScaleTriggerEvent(SELECTOR, STACK_ID, INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO,
                adjustmentTypeWithThreshold);

        when(stackScalabilityCondition.isScalable(stack, INSTANCE_GROUP_NAME)).thenReturn(true);

        List<CloudResourceStatus> resourceStatuses = List.of(cloudResourceStatus);
        when(resourceService.getAllAsCloudResourceStatus(STACK_ID)).thenReturn(resourceStatuses);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, Map.of());

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, ADJUSTMENT_ZERO, INSTANCE_GROUP_NAME, adjustmentTypeWithThreshold);
        verifyEventForUpscaleStackResult(resourceStatuses);
    }

}