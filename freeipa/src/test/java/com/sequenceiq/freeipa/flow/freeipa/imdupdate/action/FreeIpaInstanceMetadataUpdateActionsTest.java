package com.sequenceiq.freeipa.flow.freeipa.imdupdate.action;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_INSTANCE_METADATA_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_INSTANCE_METADATA_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_INSTANCE_METADATA_UPDATE_STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@ExtendWith(MockitoExtension.class)
class FreeIpaInstanceMetadataUpdateActionsTest {

    @InjectMocks
    private FreeIpaInstanceMetadataUpdateActions underTest;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    private StackContext context;

    @BeforeEach
    void setUp() {
        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void stackInstanceMetadataUpdateSendsStartedNotification() throws Exception {
        FreeIpaInstanceMetadataUpdateTriggerEvent payload =
                new FreeIpaInstanceMetadataUpdateTriggerEvent("selector", 1L, IMDS_HTTP_TOKEN_REQUIRED);
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateTriggerEvent> action =
                (AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateTriggerEvent>) underTest.stackInstanceMetadataUpdate();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_INSTANCE_METADATA_UPDATE_STARTED,
                List.of(IMDS_HTTP_TOKEN_REQUIRED.name()));
    }

    @Test
    void instanceMetadataUpdateFinishedSendsFinishedNotification() throws Exception {
        FreeIpaInstanceMetadataUpdateResult payload = new FreeIpaInstanceMetadataUpdateResult(1L);
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateResult> action =
                (AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateResult>) underTest.instanceMetadataUpdateFinished();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_INSTANCE_METADATA_UPDATE_FINISHED);
    }

    @Test
    void instanceMetadataUpdateFailedSendsFailedNotification() throws Exception {
        FreeIpaInstanceMetadataUpdateFailureEvent payload = new FreeIpaInstanceMetadataUpdateFailureEvent(1L, new Exception("boom"));
        Map<Object, Object> variables = new HashMap<>();

        AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateFailureEvent> action =
                (AbstractFreeIpaInstanceMetadataUpdateAction<FreeIpaInstanceMetadataUpdateFailureEvent>) underTest.instanceMetadataUpdateFailedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_INSTANCE_METADATA_UPDATE_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}

