package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.ADJUSTMENT_WITH_THRESHOLD;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_ADJUSTMENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.HOST_GROUP_WITH_HOSTNAMES;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.NETWORK_SCALE_DETAILS;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.REPAIR;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.AbstractStackUpscaleAction.TRIGGERED_VARIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
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
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpdateDomainDnsResolverResult;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
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

    private static final String VARIANT = "VARIANT";

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackUpscaleService stackUpscaleService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private StackUpscaleActions underTest;

    @Mock
    private StackDtoService stackDtoService;

    private StackScalingFlowContext context;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stack;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

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
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        context = new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT),
                Map.of(), Map.of(), false, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT.longValue()));
    }

    private AbstractStackUpscaleAction<UpdateDomainDnsResolverResult> getPrevalidateAction() {
        AbstractStackUpscaleAction<UpdateDomainDnsResolverResult> action = (AbstractStackUpscaleAction<UpdateDomainDnsResolverResult>) underTest.prevalidate();
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
        UpdateDomainDnsResolverResult payload = new UpdateDomainDnsResolverResult(STACK_ID);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackUpscaleService.getInstanceCountToCreate(stackDto, INSTANCE_GROUP_NAME, ADJUSTMENT, false)).thenReturn(ADJUSTMENT);

        Stack updatedStack = mock(Stack.class);
        when(instanceMetaDataService.saveInstanceAndGetUpdatedStack(stackDto, Map.of(INSTANCE_GROUP_NAME, 3), Map.of(), false, false,
                context.getStackNetworkScaleDetails())).thenReturn(updatedStack);
        CloudStack convertedCloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(updatedStack)).thenReturn(convertedCloudStack);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, createVariables(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT), Map.of(),
                NetworkScaleDetails.getEmpty(), adjustmentTypeWithThreshold, VARIANT));

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT), adjustmentTypeWithThreshold);
        verify(stackUpscaleService).startAddInstances(stack, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT));

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
        UpdateDomainDnsResolverResult payload = new UpdateDomainDnsResolverResult(STACK_ID);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackUpscaleService.getInstanceCountToCreate(stackDto, INSTANCE_GROUP_NAME, ADJUSTMENT, false)).thenReturn(ADJUSTMENT_ZERO);

        List<CloudResourceStatus> resourceStatuses = List.of(cloudResourceStatus);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, createVariables(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT), Map.of(),
                NetworkScaleDetails.getEmpty(), adjustmentTypeWithThreshold, VARIANT));

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT), adjustmentTypeWithThreshold);
        verifyEventForExtendMetadata(resourceStatuses);
    }

    private void verifyEventForExtendMetadata(List<CloudResourceStatus> resourceStatuses) {
        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        verify(eventBus).notify("EXTEND_METADATA", event);

        Object responsePayload = payloadArgumentCaptor.getValue();
        assertThat(responsePayload).isInstanceOf(StackEvent.class);

        StackEvent stackEvent = (StackEvent) responsePayload;
        assertThat(stackEvent.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void prevalidateTestDoExecuteWhenScalingNotNeeded() throws Exception {
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, ADJUSTMENT_ZERO.longValue());
        context = new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO),
                Map.of(), Map.of(), false, adjustmentTypeWithThreshold);
        UpdateDomainDnsResolverResult payload = new UpdateDomainDnsResolverResult(STACK_ID);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackUpscaleService.getInstanceCountToCreate(stackDto, INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO, false)).thenReturn(ADJUSTMENT_ZERO);

        List<CloudResourceStatus> resourceStatuses = List.of(cloudResourceStatus);

        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(getPrevalidateAction()).doExecute(context, payload, createVariables(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO),
                Map.of(), NetworkScaleDetails.getEmpty(), adjustmentTypeWithThreshold, VARIANT));

        verify(stackUpscaleService).addInstanceFireEventAndLog(stack, Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO), adjustmentTypeWithThreshold);
        verifyEventForExtendMetadata(resourceStatuses);
    }

    @Test
    void prevalidateTestCreateContextWhenTriggeredVariantSet() {
        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails();
        UpdateDomainDnsResolverResult payload = new UpdateDomainDnsResolverResult(STACK_ID);
        Map<Object, Object> variables = createVariables(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO),
                Map.of(INSTANCE_GROUP_NAME, Set.of("hostname")), networkScaleDetails, null, VARIANT);
        new AbstractActionTestSupport<>(getPrevalidateAction()).prepareExecution(payload, variables);
        Assertions.assertEquals(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO), variables.get(HOST_GROUP_WITH_ADJUSTMENT));
        Assertions.assertEquals(Map.of(INSTANCE_GROUP_NAME, Set.of("hostname")), variables.get(HOST_GROUP_WITH_HOSTNAMES));
        Assertions.assertEquals(false, variables.get(REPAIR));
        Assertions.assertEquals(VARIANT, variables.get(TRIGGERED_VARIANT));
        Assertions.assertEquals(networkScaleDetails, variables.get(NETWORK_SCALE_DETAILS));
    }

    @Test
    void prevalidateTestCreateContextWhenTriggeredVariantNotSet() {
        NetworkScaleDetails networkScaleDetails = new NetworkScaleDetails();
        UpdateDomainDnsResolverResult payload = new UpdateDomainDnsResolverResult(STACK_ID);
        Map<Object, Object> variables = createVariables(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO),
                Map.of(INSTANCE_GROUP_NAME, Set.of("hostname")),
                networkScaleDetails, null, null);
        new AbstractActionTestSupport<>(getPrevalidateAction()).prepareExecution(payload, variables);
        Assertions.assertEquals(Map.of(INSTANCE_GROUP_NAME, ADJUSTMENT_ZERO), variables.get(HOST_GROUP_WITH_ADJUSTMENT));
        Assertions.assertEquals(Map.of(INSTANCE_GROUP_NAME, Set.of("hostname")), variables.get(HOST_GROUP_WITH_HOSTNAMES));
        Assertions.assertEquals(false, variables.get(REPAIR));
        Assertions.assertNull(variables.get(TRIGGERED_VARIANT));
        Assertions.assertEquals(networkScaleDetails, variables.get(NETWORK_SCALE_DETAILS));
    }

    public Map<Object, Object> createVariables(Map<String, Integer> hostGroupsWithAdjustment, Map<String, Set<String>> hostGroupsWithHostNames,
            NetworkScaleDetails networkScaleDetails, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(HOST_GROUP_WITH_ADJUSTMENT, hostGroupsWithAdjustment);
        variables.put(HOST_GROUP_WITH_HOSTNAMES, hostGroupsWithHostNames);
        variables.put(REPAIR, false);
        if (triggeredStackVariant != null) {
            variables.put(TRIGGERED_VARIANT, triggeredStackVariant);
        }
        variables.put(NETWORK_SCALE_DETAILS, networkScaleDetails);
        variables.put(ADJUSTMENT_WITH_THRESHOLD, adjustmentTypeWithThreshold);
        return variables;
    }
}