package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class SetEncryptionProfileHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @InjectMocks
    private SetEncryptionProfileHandler underTest;

    @Mock
    private Environment environment;

    private EnableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new EnableEncryptionProfileEvent(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), 1L, "envName", "envCrn", "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testSetEncryptionProfileHandlerSuccess() {
        when(environmentService.findEnvironmentById(event.getResourceId())).thenReturn(Optional.of(environment));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(encryptionProfileService, times(1)).setEncryptionProfile(environment, event.getEncryptionProfileCrn());
        verify(environmentService, times(1)).save(environment);
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(UPDATE_SSL_CONFIG_FREEIPA_EVENT.selector(), response.getSelector());
    }

    @Test
    void testSetEncryptionProfileHandlerFailure() {
        when(environmentService.findEnvironmentById(event.getResourceId())).thenReturn(Optional.of(environment));

        doThrow(new CloudbreakServiceException("failed"))
                .when(encryptionProfileService).setEncryptionProfile(environment, event.getEncryptionProfileCrn());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(event.getResourceId(), selectable.getResourceId());
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name(), selectable.selector());
        assertEquals("failed", selectable.getException().getMessage());
    }
}
