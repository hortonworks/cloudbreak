package com.sequenceiq.freeipa.flow.instance.reboot.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_REBOOT_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_REBOOT_FINISHED;
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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootContext;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootService;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class RebootActionsNotificationTest {

    @InjectMocks
    private RebootActions underTest;

    @Mock
    private RebootService rebootService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private OperationService operationService;

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

    private RebootContext context;

    @BeforeEach
    void setUp() {
        InstanceMetaData first = new InstanceMetaData();
        first.setInstanceId("i-1");
        InstanceMetaData second = new InstanceMetaData();
        second.setInstanceId("i-2");
        context = new RebootContext(new FlowParameters("flow", "user-crn"), stack, List.of(first, second), null, null);

        doReturn(1L).when(stack).getId();
        doReturn("acc").when(stack).getAccountId();
        doReturn("crn:cdp:environments:us-west-1:test:environment:env-1").when(stack).getEnvironmentCrn();
    }

    @Test
    void rebootFinishedActionSendsFinishedNotification() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractRebootAction<HealthCheckSuccess> action = (AbstractRebootAction<HealthCheckSuccess>) underTest.rebootFinishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "operationService", operationService);

        Map<Object, Object> variables = new HashMap<>();
        variables.put(AbstractRebootAction.OPERATION_ID, "op-1");
        HealthCheckSuccess payload = new HealthCheckSuccess(1L, List.of("i-1", "i-2"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_REBOOT_FINISHED, List.of("2", "i-1,i-2"));
    }

    @Test
    void rebootFailureActionSendsFailedNotification() throws Exception {
        @SuppressWarnings("unchecked")
        AbstractRebootAction<InstanceFailureEvent> action = (AbstractRebootAction<InstanceFailureEvent>) underTest.rebootFailureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "operationService", operationService);

        Map<Object, Object> variables = new HashMap<>();
        variables.put(AbstractRebootAction.OPERATION_ID, "op-1");
        InstanceFailureEvent payload = new InstanceFailureEvent(1L, new Exception("boom"), List.of("i-1", "i-2"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), "ok")).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_REBOOT_FAILED, List.of("2", "i-1,i-2", "boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}

