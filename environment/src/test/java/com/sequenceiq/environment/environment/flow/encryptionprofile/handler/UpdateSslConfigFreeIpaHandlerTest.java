package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_IN_CLUSTERS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateSslConfigFreeIpaHandlerTest {

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @InjectMocks
    private UpdateSslConfigFreeIpaHandler underTest;

    private EnableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new EnableEncryptionProfileEvent(UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT.name(), 1L, "envName", "envCrn", "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testUpdateSslConfigFreeIpaHandlerSuccess() {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(freeIpaPollerService, times(1)).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(UPDATE_SSL_CONFIG_IN_CLUSTERS_EVENT.selector(), response.getSelector());
    }

    @Test
    void testUpdateSslConfigFreeIpaHandlerFailure() {
        doThrow(new CloudbreakServiceException("failed"))
                .when(freeIpaPollerService).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(event.getResourceId(), selectable.getResourceId());
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name(), selectable.selector());
        assertEquals("failed", selectable.getException().getMessage());
    }
}
