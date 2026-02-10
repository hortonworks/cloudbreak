package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileValidator;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ValidateEnableEncryptionProfileHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private EncryptionProfileValidator encryptionProfileValidator;

    @InjectMocks
    private ValidateEnableEncryptionProfileHandler underTest;

    @Mock
    private Environment environment;

    private EnableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new EnableEncryptionProfileEvent(VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), 1L, "envName", "envCrn", "epCrn");
    }

    @Test
    void testSelector() {
        assertEquals(VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
    }

    @Test
    void testValidateEnableEncryptionProfileHandlerSuccess() {
        List<StackViewV4Response> stacks = List.of(new StackViewV4Response(), new StackViewV4Response());

        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(event.getResourceCrn())).thenReturn(stacks);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(encryptionProfileValidator, times(1)).validate(stacks);
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(SET_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
    }

    @Test
    void testValidateEnableEncryptionProfileHandlerFailure() {
        List<StackViewV4Response> stacks = List.of(new StackViewV4Response(), new StackViewV4Response());

        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(event.getResourceCrn())).thenReturn(stacks);

        doThrow(new CloudbreakServiceException("failed"))
                .when(encryptionProfileValidator).validate(stacks);

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertEquals(event.getResourceId(), selectable.getResourceId());
        assertEquals(FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.name(), selectable.selector());
        assertEquals("failed", selectable.getException().getMessage());
    }
}
