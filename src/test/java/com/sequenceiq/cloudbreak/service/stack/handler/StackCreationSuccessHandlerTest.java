package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationSuccess;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;

import reactor.core.Reactor;
import reactor.event.Event;

public class StackCreationSuccessHandlerTest {

    private static final String STACK_NAME = "stackName";
    private static final String DUMMY_EMAIL = "gipszjakab@myemail.com";
    private static final String DETAILED_MESSAGE = "message";

    @InjectMocks
    private StackCreationSuccessHandler underTest;

    @Mock
    private RetryingStackUpdater stackUpdater;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private Reactor reactor;

    private Event<StackCreationSuccess> event;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new StackCreationSuccessHandler();
        MockitoAnnotations.initMocks(this);
        event = createEvent();
        stack = createStack();
    }

    @Test
    public void testAcceptStackCreationSuccessEvent() {
        // GIVEN
        given(stackUpdater.updateAmbariIp(anyLong(), anyString())).willReturn(stack);
        given(stackUpdater.updateStackStatus(anyLong(), any(Status.class))).willReturn(stack);
        given(stackUpdater.updateStackStatusReason(anyLong(), anyString())).willReturn(stack);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any());
        // WHEN
        underTest.accept(event);
        // THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any());
        verify(reactor, times(1)).notify(any(ReactorConfig.class), any(Event.class));
    }

    private Event<StackCreationSuccess> createEvent() {
        StackCreationSuccess data = new StackCreationSuccess(1L, DETAILED_MESSAGE);
        return new Event<StackCreationSuccess>(data);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setName(STACK_NAME);
        stack.setOwner(DUMMY_EMAIL);
        return stack;
    }
}
