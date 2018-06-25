package com.sequenceiq.cloudbreak.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class StackUpdaterTest {

    @Mock
    private StackStatusRepository stackStatusRepository;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    public void updateStackStatusWithoutStatusReasonThenNoNotificationSentOnWebsocket() {
        Stack stack = TestUtil.stack();

        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        when(stackRepository.findById(anyLong())).thenReturn(Optional.of(stack));
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);

        Stack newStack = underTest.updateStackStatus(1L, DetailedStackStatus.DELETE_COMPLETED);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals("", newStack.getStatusReason());
        verify(cloudbreakEventService, times(0)).fireCloudbreakEvent(anyLong(), anyString(), anyString());
    }

    @Test
    public void updateStackStatusAndReasonThenNotificationSentOnWebsocket() {
        Stack stack = TestUtil.stack();

        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        String newStatusReason = "test";
        when(stackRepository.findById(anyLong())).thenReturn(Optional.of(stack));
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);

        Stack newStack = underTest.updateStackStatus(1L, newStatus, newStatusReason);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals(newStatusReason, newStack.getStatusReason());
    }

}