package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.CURRENT_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.DATABASE_STACK;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.ENVIRONMENT_CRN;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.REGION;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.RESOURCE_ID;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.TARGET_INSTANCE_TYPE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.cloudContext;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.handlerEvent;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.handler.ChangeInstanceTypeHandlerTestSupport.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeValidationFailedEvent;
import com.sequenceiq.redbeams.service.validation.ChangeInstanceTypeValidator;

@ExtendWith(MockitoExtension.class)
class ValidateChangeInstanceTypeHandlerTest {

    private final DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);

    @Mock
    private ChangeInstanceTypeValidator changeInstanceTypeValidator;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Event<Object>> eventCaptor;

    @InjectMocks
    private ValidateChangeInstanceTypeHandler underTest;

    @Test
    void testSelector() {
        assertEquals(VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER.event(), underTest.selector());
    }

    @Test
    void testValidationRunsAgainstTheLiveState() {
        Selectable result = underTest.doAccept(createHandlerEvent());

        verify(changeInstanceTypeValidator).validate(DATABASE_STACK, liveState, TARGET_INSTANCE_TYPE, ENVIRONMENT_CRN, "AWS", REGION);
        ChangeInstanceTypeEvent event = assertInstanceOf(ChangeInstanceTypeEvent.class, result);
        assertEquals(CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT.event(), event.selector());
        assertEquals(RESOURCE_ID, event.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, event.getTargetInstanceType());
        assertSame(liveState, event.getLiveState());
    }

    @Test
    void testValidationFailureFailsTheFlow() {
        HandlerEvent<ChangeInstanceTypeHandlerRequest> handlerEvent = createHandlerEvent();
        BadRequestException exception = new BadRequestException("invalid target");
        doThrow(exception).when(changeInstanceTypeValidator).validate(DATABASE_STACK, liveState, TARGET_INSTANCE_TYPE, ENVIRONMENT_CRN, "AWS", REGION);

        underTest.accept(handlerEvent.getEvent());

        verify(eventBus).notify(eq(CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT.event()), eventCaptor.capture());
        ChangeInstanceTypeValidationFailedEvent failedEvent =
                assertInstanceOf(ChangeInstanceTypeValidationFailedEvent.class, eventCaptor.getValue().getData());
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
        assertSame(exception, failedEvent.getException());
    }

    private HandlerEvent<ChangeInstanceTypeHandlerRequest> createHandlerEvent() {
        return handlerEvent(VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER.event(), cloudContext("AWS"), liveState);
    }
}
