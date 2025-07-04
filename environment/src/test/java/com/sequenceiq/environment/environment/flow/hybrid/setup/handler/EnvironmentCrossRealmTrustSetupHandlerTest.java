package com.sequenceiq.environment.environment.flow.hybrid.setup.handler;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINISH_TRUST_SETUP_EVENT;
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
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentCrossRealmTrustSetupHandlerTest {

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HandlerEvent<EnvironmentCrossRealmTrustSetupEvent> event;

    @Mock
    private DescribeFreeIpaResponse freeIpa;

    @InjectMocks
    private EnvironmentCrossRealmTrustSetupHandler handler;

    private EnvironmentCrossRealmTrustSetupEvent eventData;

    @BeforeEach
    void setUp() {
        eventData = EnvironmentCrossRealmTrustSetupEvent.builder()
                .withAccountId("1")
                .withResourceId(100L)
                .withResourceCrn("crn:env:100")
                .withRemoteEnvironmentCrn("crn:remote:200")
                .withResourceName("env-name")
                .withRealm("REALM")
                .withFqdn("env.example.com")
                .withIp("1.2.3.4")
                .withTrustSecret("secret")
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

        verify(environmentService).updateRemoteEnvironmentCrn(
                eq(eventData.getAccountId()),
                eq(eventData.getResourceCrn()),
                eq(eventData.getRemoteEnvironmentCrn())
        );

        verify(freeIpaPollerService).waitForCrossRealmTrustSetup(
                eq(eventData.getResourceId()),
                eq(eventData.getResourceCrn()),
                any(PrepareCrossRealmTrustRequest.class)
        );

        assertThat(result.selector()).isEqualTo(FINISH_TRUST_SETUP_EVENT.selector());
    }

    @Test
    void testDoAcceptNullStatus() {
        when(freeIpaService.describe(eventData.getResourceCrn()))
                .thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(null);
        doNothing().when(environmentService).updateRemoteEnvironmentCrn(any(), any(), any());
        Selectable result = handler.doAccept(event);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustSetupFailedEvent.class);
    }

    @Test
    void testDoAcceptNonPreparableStatus() {
        when(freeIpaService.describe(eventData.getResourceCrn()))
                .thenReturn(Optional.of(freeIpa));
        when(freeIpa.getStatus()).thenReturn(Status.STOPPED);
        when(freeIpa.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE);

        Selectable result = handler.doAccept(event);

        assertThat(result).isInstanceOf(EnvironmentCrossRealmTrustSetupFailedEvent.class);
    }
}
