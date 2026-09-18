package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UnsetEncryptionProfileHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private UnsetEncryptionProfileHandler underTest;

    private DisableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new DisableEncryptionProfileEvent(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), 1L, "envName", "envCrn");
    }

    @Test
    void testSelector() {
        assertEquals(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testUnsetEncryptionProfileHandlerSuccess() {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(environmentService, times(1)).disableEncryptionProfile(event.getResourceCrn());
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_EVENT.selector(), response.getSelector());
    }

    @Test
    void testUnsetEncryptionProfileHandlerFailurePropagatesException() {
        doThrow(new CloudbreakServiceException("failed"))
                .when(environmentService).disableEncryptionProfile(event.getResourceCrn());

        assertThatThrownBy(() -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasMessage("failed");
    }
}
