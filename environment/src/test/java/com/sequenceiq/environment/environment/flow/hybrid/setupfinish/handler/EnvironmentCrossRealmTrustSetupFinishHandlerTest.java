package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_HANDLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustSetupFinishHandlerTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String RESOURCE_CRN = "crn:env:freeipa:example";

    private static final String RESOURCE_NAME = "env-1";

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private DescribeFreeIpaResponse freeIpa;

    @Mock
    private Status freeIpaStatus;

    @Mock
    private EventBus eventBus;

    @Mock
    private Event<EnvironmentCrossRealmTrustSetupFinishEvent> event;

    @InjectMocks
    private EnvironmentCrossRealmTrustSetupFinishHandler handler;

    @Test
    void testDoAcceptWhenFreeIpaStatusIsValidShouldReturnFinishEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent eventData = createEvent();

        ReflectionTestUtils.setField(handler, null, eventBus, EventBus.class);

        when(event.getData()).thenReturn(eventData);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(freeIpaStatus);
        when(freeIpa.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE);
        when(freeIpaStatus.isCrossRealmFinishable()).thenReturn(true);

        handler.accept(event);

        verify(freeIpaPollerService).waitForCrossRealmFinish(eq(RESOURCE_ID), eq(RESOURCE_CRN), any(FinishCrossRealmTrustRequest.class));
    }

    @Test
    void testDoAcceptWhenCrossRealmNotFinishableShouldReturnFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent eventData = createEvent();

        ReflectionTestUtils.setField(handler, null, eventBus, EventBus.class);

        when(event.getData()).thenReturn(eventData);
        when(freeIpaService.describe(RESOURCE_CRN)).thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(freeIpaStatus);
        when(freeIpa.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE);
        when(freeIpaStatus.isCrossRealmFinishable()).thenReturn(false);

        handler.accept(event);
    }

    @Test
    void testSelectorReturnsCorrectValue() {
        String selector = handler.selector();
        assertEquals(SETUP_FINISH_TRUST_HANDLER.selector(), selector);
    }

    @Test
    void testDefaultFailureEventShouldWrapInFailedEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent data = createEvent();
        Exception exception = new RuntimeException("failure");

        when(event.getData()).thenReturn(data);
        Selectable result = handler.defaultFailureEvent(RESOURCE_ID, exception, event);

        assertTrue(result instanceof EnvironmentCrossRealmTrustSetupFinishFailedEvent);
        EnvironmentCrossRealmTrustSetupFinishFailedEvent failed = (EnvironmentCrossRealmTrustSetupFinishFailedEvent) result;
        assertEquals(TRUST_SETUP_FINISH_VALIDATION_FAILED, failed.getEnvironmentStatus());
        assertEquals(exception, failed.getException());
    }

    private EnvironmentCrossRealmTrustSetupFinishEvent createEvent() {
        EnvironmentCrossRealmTrustSetupFinishEvent event = EnvironmentCrossRealmTrustSetupFinishEvent.builder()
                .withResourceCrn(RESOURCE_CRN)
                .withResourceId(RESOURCE_ID)
                .withResourceName(RESOURCE_NAME)
                .build();
        return event;
    }
}