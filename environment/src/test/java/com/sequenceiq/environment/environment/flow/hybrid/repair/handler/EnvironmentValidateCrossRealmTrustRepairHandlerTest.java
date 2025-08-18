package com.sequenceiq.environment.environment.flow.hybrid.repair.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairHandlerSelectors.TRUST_REPAIR_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.TRUST_REPAIR_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairEvent;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidateCrossRealmTrustRepairHandlerTest {

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustRepairEvent> handlerEvent;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private EnvironmentValidateCrossRealmTrustRepairHandler handler;

    private EnvironmentCrossRealmTrustRepairEvent eventData;

    @BeforeEach
    void setUp() {
        handler = new EnvironmentValidateCrossRealmTrustRepairHandler(environmentService);

        eventData = EnvironmentCrossRealmTrustRepairEvent.builder()
                .withResourceId(100L)
                .withResourceCrn("crn:env:100")
                .withResourceName("env-name")
                .build();

    }

    @Test
    void testDoAcceptReturnsNextEvent() {
        when(handlerEvent.getData()).thenReturn(eventData);

        Selectable result = handler.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustRepairEvent.class);
        EnvironmentCrossRealmTrustRepairEvent newEvent = (EnvironmentCrossRealmTrustRepairEvent) result;

        assertThat(newEvent.selector()).isEqualTo(TRUST_REPAIR_EVENT.selector());
        assertThat(newEvent.getResourceCrn()).isEqualTo(eventData.getResourceCrn());
        assertThat(newEvent.getResourceId()).isEqualTo(eventData.getResourceId());
    }

    @Test
    void testSelectorReturnsCorrectSelector() {
        String selector = handler.selector();
        assertThat(selector).isEqualTo(TRUST_REPAIR_VALIDATION_HANDLER.selector());
    }

    @Test
    void testDefaultFailureEventReturnsCorrectEvent() {
        Exception ex = new RuntimeException("some error");
        Event<EnvironmentCrossRealmTrustRepairEvent> mockEvent = mock(Event.class);
        when(mockEvent.getData()).thenReturn(eventData);

        Selectable result = handler.defaultFailureEvent(123L, ex, mockEvent);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustRepairFailedEvent.class);
        assertThat(((EnvironmentCrossRealmTrustRepairFailedEvent) result).getEnvironmentStatus()).isEqualTo(TRUST_REPAIR_VALIDATION_FAILED);
    }
}
