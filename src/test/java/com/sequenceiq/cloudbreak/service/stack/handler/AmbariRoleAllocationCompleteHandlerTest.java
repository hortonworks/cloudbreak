package com.sequenceiq.cloudbreak.service.stack.handler;

import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.event.Event;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class AmbariRoleAllocationCompleteHandlerTest {
    public static final long STACK_ID = 1L;
    public static final String DUMMY_IP = "dummyIp";

    @InjectMocks
    private AmbariRoleAllocationCompleteHandler underTest;

    private Event<AmbariRoleAllocationComplete> event;

    @Mock
    private AmbariStartupListener ambariStartupListener;

    @Before
    public void setUp() {
        underTest = new AmbariRoleAllocationCompleteHandler();
        MockitoAnnotations.initMocks(this);
        event = createEvent();
    }

    @Test
    public void testAcceptAmbariRoleAllocationCompleteEvent() {
        // GIVEN
        doNothing().when(ambariStartupListener).waitForAmbariServer(STACK_ID, DUMMY_IP);
        // WHEN
        underTest.accept(event);
        // THEN
        verify(ambariStartupListener, times(1)).waitForAmbariServer(STACK_ID, DUMMY_IP);
    }

    private Event<AmbariRoleAllocationComplete> createEvent() {
        return new Event<AmbariRoleAllocationComplete>(new AmbariRoleAllocationComplete(STACK_ID, DUMMY_IP));
    }
}
