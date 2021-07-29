package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.MessageFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import reactor.bus.Event;
import reactor.bus.EventBus;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class FreeIpaUpscaleActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String ACTION_PAYLOAD_SELECTOR = "selector";

    private static final Long STACK_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final String FAILURE_EVENT = "failureEvent";

    private static final Integer INSTANCE_GROUP_COUNT = 3;

    @Mock
    private StateContext stateContext;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachine stateMachine;

    @Mock
    private State state;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowEvent failureEvent;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Captor
    private ArgumentCaptor<String> selectorArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    @Mock
    private Tracer tracer;

    @Mock
    private Tracer.SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private SpanContext spanContext;

    @Mock
    private StackService stackService;

    @Mock
    private FlowEvent flowEvent;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private PrivateIdProvider privateIdProvider;

    @Mock
    private ResourceToCloudResourceConverter resourceConverter;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private FreeIpaUpscaleActions underTest;

    private UpscaleEvent actionPayload;

    @BeforeEach
    void setUp() {
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN, null);
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_PARAMETERS.name()))
                .thenReturn(flowParameters);
        actionPayload = new UpscaleEvent(ACTION_PAYLOAD_SELECTOR, STACK_ID, INSTANCE_GROUP_COUNT, true,
                false, false, "op-id");
        when(stateContext.getMessageHeader(MessageFactory.HEADERS.DATA.name()))
                .thenReturn(actionPayload);
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        Map<Object, Object> variables = new HashMap<>();
        variables.put("REPAIR", true);
        variables.put("INSTANCE_COUNT_BY_GROUP", INSTANCE_GROUP_COUNT);
        when(extendedState.getVariables()).thenReturn(variables);
        when(stateContext.getStateMachine()).thenReturn(stateMachine);
        when(stateMachine.getState()).thenReturn(state);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        when(stateContext.getEvent()).thenReturn(flowEvent);
        when(tracer.buildSpan(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.addReference(anyString(), any())).thenReturn(spanBuilder);
        when(spanBuilder.ignoreActiveSpan()).thenReturn(spanBuilder);
        when(spanBuilder.start()).thenReturn(span);
        when(tracer.activateSpan(span)).thenReturn(scope);
        when(span.context()).thenReturn(spanContext);
        when(flowEvent.name()).thenReturn("eventName");
    }

    @Test
    void startingAction() {
        Action<?, ?> action = configureAction(underTest::addInstancesAction);

        Stack stack = new Stack();
        stack.setId(1L);
        stack.setRegion("region");
        stack.setAvailabilityZone("az");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("ipaserver");
        freeIpa.setDomain("foo.bar.baz");
        when(freeIpaService.findByStackId(anyLong())).thenReturn(freeIpa);

        when(privateIdProvider.getFirstValidPrivateId(any()))
                .thenReturn(Long.valueOf(INSTANCE_GROUP_COUNT));

        action.execute(stateContext);
        verifyNoMoreInteractions(stackService);

        for (InstanceGroup instanceGroup1 : stack.getInstanceGroups()) {
            int i = INSTANCE_GROUP_COUNT;
            for (InstanceMetaData instanceMetaData : instanceGroup1.getInstanceMetaData()) {
                assertThat(instanceMetaData.getDiscoveryFQDN())
                        .isEqualTo("ipaserver" + i++ + ".foo.bar.baz");
            }
        }
    }

    private Action<?, ?> configureAction(Supplier<Action<?, ?>> actionSupplier) {
        Action<?, ?> action = actionSupplier.get();
        assertThat(action).isNotNull();
        setActionPrivateFields(action);
        AbstractAction abstractAction = (AbstractAction) action;
        abstractAction.setFailureEvent(failureEvent);
        return action;
    }

    private void setActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils
                .setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, tracer, Tracer.class);
        ReflectionTestUtils.setField(action, null, stackService, StackService.class);
        ReflectionTestUtils
                .setField(action, null, credentialConverter, CredentialToCloudCredentialConverter.class);
        ReflectionTestUtils
                .setField(action, null, cloudStackConverter, StackToCloudStackConverter.class);
        ReflectionTestUtils.setField(action, null, credentialService, CredentialService.class);
    }
}