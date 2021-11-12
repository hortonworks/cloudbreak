package com.sequenceiq.cloudbreak.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;

@RunWith(MockitoJUnitRunner.class)
public class StackUpdaterTest {

    @Mock
    private StackService stackService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Mock
    private UsageLoggingUtil usageLoggingUtil;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    public void skipStackStatusUpdateWhenActualStatusEqualsNewStatus() {
        Stack stack = TestUtil.stack();

        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        Stack modifiedStack = underTest.updateStackStatus(1L, newStatus, "newReason");
        assertEquals(stack.getStatus(), modifiedStack.getStatus());
        assertEquals(newStatus.getStatus(), modifiedStack.getStatus());
        verify(stackService, never()).save(any());
    }

    @Test
    public void skipStackStatusUpdateWhenStatusIsDeleteCompleted() {
        Stack stack = TestUtil.stack(Status.DELETE_COMPLETED);

        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);

        Stack modifiedStack = underTest.updateStackStatus(1L, newStatus, "newReason");
        assertEquals(Status.DELETE_COMPLETED, modifiedStack.getStatus());
        verify(stackService, never()).save(any());
    }

    @Test
    public void updateStackStatusAndReason() {
        Stack stack = TestUtil.stack(TestUtil.cluster());

        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        String newStatusReason = "test";
        when(stackService.getByIdWithTransaction(anyLong())).thenReturn(stack);
        when(stackService.save(any(Stack.class))).thenReturn(stack);

        Stack newStack = underTest.updateStackStatus(1L, newStatus, newStatusReason);
        assertEquals(newStatus.getStatus(), newStack.getStatus());
        assertEquals(newStatusReason, newStack.getStatusReason());
        verify(stackService, times(1)).save(eq(stack));
        verify(clusterService, times(1)).save(eq(stack.getCluster()));
        verify(usageLoggingUtil, times(1)).logClusterStatusChangeUsageEvent(eq(Status.AVAILABLE), eq(Status.DELETE_COMPLETED), eq(stack.getCluster()));
    }

}