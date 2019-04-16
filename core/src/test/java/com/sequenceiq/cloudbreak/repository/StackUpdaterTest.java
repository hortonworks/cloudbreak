package com.sequenceiq.cloudbreak.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackUpdaterTest {

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Mock
    private Clock clock;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    public void updateStackStatusWithoutStatusReasonThenNoNotificationSentOnWebsocket() {
        Stack stack = TestUtil.stack();

        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);
        when(stackService.save(any(Stack.class))).thenReturn(stack);

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
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);
        when(stackService.save(any(Stack.class))).thenReturn(stack);

        Stack newStack = underTest.updateStackStatus(1L, newStatus, newStatusReason);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals(newStatusReason, newStack.getStatusReason());
    }

}