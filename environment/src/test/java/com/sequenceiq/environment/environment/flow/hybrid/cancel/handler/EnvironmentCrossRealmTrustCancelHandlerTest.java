package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINISH_TRUST_CANCEL_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustCancelHandlerTest {

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> event;

    @Mock
    private DescribeFreeIpaResponse freeIpa;

    @InjectMocks
    private EnvironmentCrossRealmTrustCancelHandler handler;

    private EnvironmentCrossRealmTrustCancelEvent eventData;

    @BeforeEach
    void setUp() {
        eventData = EnvironmentCrossRealmTrustCancelEvent.builder()
                .withResourceId(100L)
                .withResourceCrn("crn:env:100")
                .withResourceName("env-name")
                .build();

        when(event.getData()).thenReturn(eventData);
    }

    @Test
    void testDoAcceptSuccess() {
        when(freeIpaService.describe(eventData.getResourceCrn()))
                .thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(Status.AVAILABLE);
        when(freeIpa.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE);

        Selectable result = handler.doAccept(event);

        verify(environmentService).removeRemoteEnvironmentCrn(
                eq(eventData.getResourceCrn())
        );

        verify(freeIpaPollerService).waitForCrossRealmTrustCancel();

        assertThat(result.selector()).isEqualTo(FINISH_TRUST_CANCEL_EVENT.selector());
    }

    @Test
    void testDoAcceptNullStatus() {
        when(freeIpaService.describe(eventData.getResourceCrn()))
                .thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(null);
        doNothing().when(environmentService).removeRemoteEnvironmentCrn(any());
        Selectable result = handler.doAccept(event);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustCancelFailedEvent.class);
    }
}
