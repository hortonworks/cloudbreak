package com.sequenceiq.environment.environment.flow.hybrid.setup.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_VALIDATION_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.TRUST_SETUP_EVENT;
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
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidateCrossRealmTrustSetupHandlerTest {

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustSetupEvent> handlerEvent;

    @InjectMocks
    private EnvironmentValidateCrossRealmTrustSetupHandler handler;

    private EnvironmentCrossRealmTrustSetupEvent eventData;

    @BeforeEach
    void setUp() {
        handler = new EnvironmentValidateCrossRealmTrustSetupHandler();

        eventData = EnvironmentCrossRealmTrustSetupEvent.builder()
                .withAccountId("1")
                .withResourceId(100L)
                .withResourceCrn("crn:env:100")
                .withRemoteEnvironmentCrn("crn:remote:200")
                .withResourceName("env-name")
                .withKdcRealm("REALM")
                .withKdcFqdn("env.example.com")
                .withKdcIp("1.2.3.4")
                .withTrustSecret("secret")
                .build();

    }

    @Test
    void testDoAcceptReturnsNextEvent() {
        when(handlerEvent.getData()).thenReturn(eventData);

        Selectable result = handler.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustSetupEvent.class);
        EnvironmentCrossRealmTrustSetupEvent newEvent = (EnvironmentCrossRealmTrustSetupEvent) result;

        assertThat(newEvent.selector()).isEqualTo(TRUST_SETUP_EVENT.selector());
        assertThat(newEvent.getResourceCrn()).isEqualTo(eventData.getResourceCrn());
        assertThat(newEvent.getResourceId()).isEqualTo(eventData.getResourceId());
        assertThat(newEvent.getKdcRealm()).isEqualTo(eventData.getKdcRealm());
    }

    @Test
    void testDoAcceptWithExceptionReturnsFailureEvent() {
        EnvironmentCrossRealmTrustSetupEvent environmentCrossRealmTrustSetupEvent
                = mock(EnvironmentCrossRealmTrustSetupEvent.class);

        when(environmentCrossRealmTrustSetupEvent.toBuilder()).thenThrow(new RuntimeException("forced failure"));
        when(handlerEvent.getData()).thenReturn(environmentCrossRealmTrustSetupEvent);

        Selectable result = handler.doAccept(handlerEvent);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustSetupFailedEvent.class);
        EnvironmentCrossRealmTrustSetupFailedEvent failedEvent = (EnvironmentCrossRealmTrustSetupFailedEvent) result;

        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(TRUST_SETUP_VALIDATION_FAILED);
        assertThat(failedEvent.getException().getMessage()).contains("forced failure");
    }

    @Test
    void testSelectorReturnsCorrectSelector() {
        String selector = handler.selector();
        assertThat(selector).isEqualTo(TRUST_SETUP_VALIDATION_HANDLER.selector());
    }

    @Test
    void testDefaultFailureEventReturnsCorrectEvent() {
        Exception ex = new RuntimeException("some error");
        Event<EnvironmentCrossRealmTrustSetupEvent> mockEvent = mock(Event.class);
        when(mockEvent.getData()).thenReturn(eventData);

        Selectable result = handler.defaultFailureEvent(123L, ex, mockEvent);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustSetupFailedEvent.class);
        assertThat(((EnvironmentCrossRealmTrustSetupFailedEvent) result).getEnvironmentStatus()).isEqualTo(TRUST_SETUP_VALIDATION_FAILED);
    }
}
