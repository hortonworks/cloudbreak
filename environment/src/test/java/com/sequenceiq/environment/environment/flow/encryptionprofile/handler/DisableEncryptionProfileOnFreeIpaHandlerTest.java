package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DisableEncryptionProfileOnFreeIpaHandlerTest {

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private DisableEncryptionProfileOnFreeIpaHandler underTest;

    private DisableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new DisableEncryptionProfileEvent(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT.name(), 1L, "envName", "envCrn");
    }

    @Test
    void testSelector() {
        assertEquals(DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT.name(), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("failed"), new Event<>(event));
        assertEquals(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector(), response.getSelector());
        assertEquals("failed", response.getException().getMessage());
        assertEquals(event.getResourceCrn(), ((DisableEncryptionProfileFailedEvent) response).getResourceCrn());
    }

    @Test
    void testDisableEncryptionProfileOnFreeIpaSuccess() {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        verify(freeIpaPollerService, times(1)).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());
        assertEquals(event.getResourceId(), response.getResourceId());
        assertEquals(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT.selector(), response.getSelector());
        assertInstanceOf(DisableEncryptionProfileEvent.class, response);
    }

    @Test
    void testDisableEncryptionProfileOnFreeIpaFailureFailsTheFlow() {
        doThrow(new FreeIpaOperationFailedException("salt boom"))
                .when(freeIpaPollerService).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());

        FreeIpaOperationFailedException exception = assertThrows(FreeIpaOperationFailedException.class,
                () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));

        assertEquals("salt boom", exception.getMessage());
    }

    @Test
    void testRetryAfterFreeIpaFailureReplaysFreeIpaStepAndTriggersStacksOnce() throws IOException {
        ReflectionTestUtils.setField(underTest, "eventBus", eventBus);
        doThrow(new FreeIpaOperationFailedException("salt boom"))
                .doNothing()
                .when(freeIpaPollerService).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());
        DisableEncryptionProfileEvent replayedEvent = JsonUtil.readValue(JsonUtil.writeValueAsString(event), DisableEncryptionProfileEvent.class);

        underTest.accept(new Event<>(event));
        underTest.accept(new Event<>(replayedEvent));

        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus, times(2)).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(List.of(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector(), DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT.selector()),
                selectorCaptor.getAllValues());
        verify(freeIpaPollerService, times(2)).waitForSaltUpdate(event.getResourceId(), event.getResourceCrn());
        assertEquals(event.getResourceCrn(), replayedEvent.getResourceCrn());
        assertTrue(replayedEvent.getSkippedClusters().isEmpty());
    }
}
