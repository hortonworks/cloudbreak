package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackStatus;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        StackStatus testStackStatus = new StackStatus(stack, newStatus.getStatus(), "", newStatus);
        when(stackStatusRepository.save(any(StackStatus.class))).thenReturn(testStackStatus);
        when(stackRepository.findOne(anyLong())).thenReturn(stack);
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);
        doNothing().when(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        when(statusToPollGroupConverter.convert(newStatus.getStatus())).thenReturn(PollGroup.POLLABLE);

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
        StackStatus testStackStatus = new StackStatus(stack, newStatus.getStatus(), newStatusReason, newStatus);
        when(stackStatusRepository.save(any(StackStatus.class))).thenReturn(testStackStatus);
        when(stackRepository.findOne(anyLong())).thenReturn(stack);
        when(stackRepository.save(any(Stack.class))).thenReturn(stack);
        doNothing().when(cloudbreakEventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        when(statusToPollGroupConverter.convert(newStatus.getStatus())).thenReturn(PollGroup.POLLABLE);

        Stack newStack = underTest.updateStackStatus(1L, newStatus, newStatusReason);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals(newStatusReason, newStack.getStatusReason());
    }

}