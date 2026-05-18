package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_SALT_UPDATE_FINISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class SaltUpdateFinishedActionTest {

    @Mock
    private OperationService operationService;

    @Mock
    private StackUpdater stackUpdater;

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

    private SaltUpdateFinishedAction underTest;

    private StackContext context;

    @BeforeEach
    void setUp() {
        underTest = new SaltUpdateFinishedAction();
        ReflectionTestUtils.setField(underTest, "operationService", operationService);
        ReflectionTestUtils.setField(underTest, "stackUpdater", stackUpdater);
        ReflectionTestUtils.setField(underTest, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(underTest, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(underTest, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(underTest, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);

        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void saltUpdateFinishedActionSendsFinishedNotification() throws Exception {
        InstallFreeIpaServicesSuccess payload = new InstallFreeIpaServicesSuccess(1L);
        Map<Object, Object> variables = new HashMap<>();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(underTest).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_SALT_UPDATE_FINISHED);
    }
}

