package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.action;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
public class FreeIpaProviderTemplateUpdateActionsTest {
    private Map<Object, Object> variables;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private FreeIpaProviderTemplateUpdateActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    private StackContext context;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Mock
    private Stack stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        when(stack.getId()).thenReturn(1L);
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testLaunchTemplateUpdateAction() throws Exception {
        FreeIpaProviderTemplateUpdateEvent launchTemplateUpdateEvent =
                new FreeIpaProviderTemplateUpdateEvent(FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT.event(),
                "test", 1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), launchTemplateUpdateEvent)).when(reactorEventFactory).createEvent(any(), any());
        AbstractFreeIpaProviderTemplateUpdateAction<FreeIpaProviderTemplateUpdateEvent> action =
                (AbstractFreeIpaProviderTemplateUpdateAction<FreeIpaProviderTemplateUpdateEvent>) underTest.deploymentTemplateUpdateAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, launchTemplateUpdateEvent, variables);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_IN_PROGRESS), eq("Starting to update provider template."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(FreeIpaProviderTemplateUpdateHandlerRequest.class);
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testLaunchTemplateUpdateFinishedAction() throws Exception {
        FreeIpaProviderTemplateUpdateEvent launchTemplateUpdateEvent =
                new FreeIpaProviderTemplateUpdateEvent(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_EVENT.event(),
                "test", 1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), launchTemplateUpdateEvent)).when(reactorEventFactory).createEvent(any(), any());
        AbstractFreeIpaProviderTemplateUpdateAction<FreeIpaProviderTemplateUpdateEvent> action =
                (AbstractFreeIpaProviderTemplateUpdateAction<FreeIpaProviderTemplateUpdateEvent>) underTest.launchTemplateUpdateFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, launchTemplateUpdateEvent, variables);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.UPDATE_COMPLETE), eq("Updating Launch Template complete."));
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINALIZED_EVENT.event(), captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
