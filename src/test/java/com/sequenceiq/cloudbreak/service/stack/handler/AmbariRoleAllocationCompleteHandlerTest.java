package com.sequenceiq.cloudbreak.service.stack.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListener;

import reactor.event.Event;

public class AmbariRoleAllocationCompleteHandlerTest {
    public static final long STACK_ID = 1L;
    public static final String DUMMY_IP = "dummyIp";

    @InjectMocks
    private AmbariRoleAllocationCompleteHandler underTest;

    private Event<AmbariRoleAllocationComplete> event;

    @Mock
    private AmbariStartupListener ambariStartupListener;

    @Mock
    private StackRepository stackRepository;

    private Stack stack;

    @Before
    public void setUp() {
        underTest = new AmbariRoleAllocationCompleteHandler();
        MockitoAnnotations.initMocks(this);
        stack = ServiceTestUtils.createStack();
        given(stackRepository.findById(anyLong())).willReturn(ServiceTestUtils.createStack());
        event = createEvent();
    }

    @Test
    public void testAcceptAmbariRoleAllocationCompleteEvent() {
        // GIVEN
        doNothing().when(ambariStartupListener).waitForAmbariServer(stack.getId(), DUMMY_IP);
        // WHEN
        underTest.accept(event);
        // THEN
        verify(ambariStartupListener, times(1)).waitForAmbariServer(stack.getId(), DUMMY_IP);
    }

    private Event<AmbariRoleAllocationComplete> createEvent() {
        return new Event<AmbariRoleAllocationComplete>(new AmbariRoleAllocationComplete(new Stack(), DUMMY_IP));
    }
}
