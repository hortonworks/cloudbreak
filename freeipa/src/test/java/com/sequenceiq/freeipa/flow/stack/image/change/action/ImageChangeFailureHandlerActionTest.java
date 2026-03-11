package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_IMAGE_CHANGE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ImageChangeFailureHandlerActionTest {

    @Mock
    private ImageService imageService;

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

    private ImageChangeFailureHandlerAction underTest;

    private StackFailureContext context;

    @BeforeEach
    void setUp() {
        underTest = new ImageChangeFailureHandlerAction();
        ReflectionTestUtils.setField(underTest, "imageService", imageService);
        ReflectionTestUtils.setField(underTest, "operationService", operationService);
        ReflectionTestUtils.setField(underTest, null, stackUpdater, StackUpdater.class);
        ReflectionTestUtils.setField(underTest, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(underTest, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(underTest, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(underTest, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);

        context = new StackFailureContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn("acc").when(stack).getAccountId();
        doReturn(1L).when(stack).getId();
    }

    @Test
    void sendsBothUpgradeAndImageChangeFailureNotificationsOnUpgradeFailure() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        StackFailureEvent payload = new StackFailureEvent(1L, new Exception("boom"), ERROR);
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);

        doReturn(operation).when(operationService).failOperation("acc", "op-1", "boom");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        new AbstractActionTestSupport<>(underTest).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_FAILED, List.of("boom"));
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_IMAGE_CHANGE_FAILED, List.of("boom"));
    }
}

