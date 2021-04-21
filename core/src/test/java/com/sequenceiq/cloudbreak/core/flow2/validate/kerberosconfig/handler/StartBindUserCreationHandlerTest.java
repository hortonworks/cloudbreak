package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.StartBindUserCreationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.retry.RetryException;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class StartBindUserCreationHandlerTest {

    @Mock
    private StackViewService stackViewService;

    @Mock
    private StartBindUserCreationService startBindUserCreationService;

    @InjectMocks
    private StartBindUserCreationHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(StartBindUserCreationEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Event<StartBindUserCreationEvent> event = new Event<>(new StartBindUserCreationEvent(1L));
        Exception e = new Exception();

        StackFailureEvent result = (StackFailureEvent) underTest.defaultFailureEvent(1L, e, event);

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    public void testSuccess() {
        StackView stackView = new StackView();
        stackView.setId(1L);
        when(stackViewService.getById(1L)).thenReturn(stackView);
        Event<StartBindUserCreationEvent> event = new Event<>(new StartBindUserCreationEvent(1L));
        StackEvent expectedEvent = new StackEvent(1L);
        when(startBindUserCreationService.startBindUserCreation(stackView)).thenReturn(expectedEvent);

        Selectable result = underTest.doAccept(new HandlerEvent<>(event));

        assertEquals(expectedEvent, result);
    }

    @Test
    public void testRetryExceptionReceived() {
        StackView stackView = new StackView();
        stackView.setId(1L);
        when(stackViewService.getById(1L)).thenReturn(stackView);
        Event<StartBindUserCreationEvent> event = new Event<>(new StartBindUserCreationEvent(1L));
        RetryException retryException = new RetryException();
        when(startBindUserCreationService.startBindUserCreation(stackView)).thenThrow(retryException);

        StackFailureEvent result = (StackFailureEvent) underTest.doAccept(new HandlerEvent<>(event));

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertEquals(retryException, result.getException());
    }
}