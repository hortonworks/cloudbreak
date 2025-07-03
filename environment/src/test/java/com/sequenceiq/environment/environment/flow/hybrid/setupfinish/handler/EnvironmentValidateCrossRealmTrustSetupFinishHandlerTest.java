package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_VALIDATION_HANDLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidateCrossRealmTrustSetupFinishHandlerTest {

    private static final Long RESOURCE_ID = 101L;

    private static final String RESOURCE_CRN = "crn:env:example:freeipa";

    private static final String RESOURCE_NAME = "env-validate-1";

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustSetupFinishEvent> handlerEvent;

    @Mock
    private Event<EnvironmentCrossRealmTrustSetupFinishEvent> event;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private EnvironmentValidateCrossRealmTrustSetupFinishHandler handler;

    private EnvironmentCrossRealmTrustSetupFinishEvent createEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent event = EnvironmentCrossRealmTrustSetupFinishEvent
                .builder()
                .withResourceName(RESOURCE_NAME)
                .withResourceId(RESOURCE_ID)
                .withResourceCrn(RESOURCE_CRN)
                .build();
        return event;
    }

    @Test
    void testSelectorReturnsCorrectValue() {
        String selector = handler.selector();
        assertEquals(SETUP_FINISH_TRUST_VALIDATION_HANDLER.selector(), selector);
    }

    @Test
    void testDoAcceptShouldReturnCrossRealmTrustSetupFinishEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent eventData = createEvent();

        ReflectionTestUtils.setField(handler, null, eventBus, EventBus.class);

        when(handlerEvent.getData()).thenReturn(eventData);

        handler.doAccept(handlerEvent);
    }

    @Test
    void testDefaultFailureEventShouldWrapExceptionInFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEvent();
        Exception exception = new RuntimeException("Validation failed");
        when(event.getData()).thenReturn(data);

        Selectable result = handler.defaultFailureEvent(RESOURCE_ID, exception, event);

        assertTrue(result instanceof EnvironmentCrossRealmTrustSetupFinishFailedEvent);
        EnvironmentCrossRealmTrustSetupFinishFailedEvent failed = (EnvironmentCrossRealmTrustSetupFinishFailedEvent) result;
        assertEquals(exception, failed.getException());
        assertEquals(TRUST_SETUP_FINISH_VALIDATION_FAILED, failed.getEnvironmentStatus());
    }
}