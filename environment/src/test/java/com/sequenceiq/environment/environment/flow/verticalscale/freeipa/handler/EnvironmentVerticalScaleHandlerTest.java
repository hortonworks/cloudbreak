package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleEvent;
import com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@ExtendWith(MockitoExtension.class)
class EnvironmentVerticalScaleHandlerTest {

    private static final String ENV_CRN = "env-crn";

    private static final long ENV_ID = 1L;

    @Mock
    private EventSender eventSender;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private Event<EnvironmentVerticalScaleEvent> event;

    @Mock
    private Event.Headers headers;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> sentEvent;

    @InjectMocks
    private EnvironmentVerticalScaleHandler underTest;

    private VerticalScaleRequest verticalScaleRequest;

    @BeforeEach
    void setUp() {
        verticalScaleRequest = new VerticalScaleRequest();
        when(event.getData()).thenReturn(EnvironmentVerticalScaleEvent.builder()
                .withResourceId(ENV_ID)
                .withResourceCrn(ENV_CRN)
                .withResourceName("env-name")
                .withFreeIPAVerticalScaleRequest(verticalScaleRequest)
                .build());
        when(event.getHeaders()).thenReturn(headers);
    }

    @Test
    void testSuccessfulScaleSendsFinishEvent() {
        when(freeIpaService.describe(ENV_CRN)).thenReturn(freeIpa(Status.STOPPED));

        underTest.accept(event);

        verify(freeIpaPollerService).waitForVerticalScale(ENV_ID, ENV_CRN, verticalScaleRequest);
        assertThat(finishEvent().selector()).isEqualTo(EnvironmentVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_FREEIPA_EVENT.selector());
    }

    @Test
    void testNonScalableStatusSendsFailedEvent() {
        when(freeIpaService.describe(ENV_CRN)).thenReturn(freeIpa(Status.DELETE_IN_PROGRESS));

        underTest.accept(event);

        verify(eventSender).sendEvent(sentEvent.capture(), any(Event.Headers.class));
        assertThat(sentEvent.getValue().selector()).contains("FAILED");
    }

    @Test
    void testFreeIpaNotFoundSendsFailedEvent() {
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        underTest.accept(event);

        verify(eventSender).sendEvent(sentEvent.capture(), any(Event.Headers.class));
        assertThat(sentEvent.getValue().selector()).contains("FAILED");
    }

    private EnvironmentVerticalScaleEvent finishEvent() {
        verify(eventSender).sendEvent(sentEvent.capture(), any(Event.Headers.class));
        assertThat(sentEvent.getValue().selector()).isEqualTo(EnvironmentVerticalScaleStateSelectors.FINISH_VERTICAL_SCALING_FREEIPA_EVENT.selector());
        return (EnvironmentVerticalScaleEvent) sentEvent.getValue();
    }

    private Optional<DescribeFreeIpaResponse> freeIpa(Status status) {
        DescribeFreeIpaResponse freeIpa = new DescribeFreeIpaResponse();
        freeIpa.setStatus(status);
        freeIpa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return Optional.of(freeIpa);
    }
}
