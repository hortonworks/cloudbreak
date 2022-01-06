package com.sequenceiq.cloudbreak.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.stack.StackBase;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterBase;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.UsageLoggingUtil;

@RunWith(MockitoJUnitRunner.class)
public class StackUpdaterTest {

    @Mock
    private StackService stackService;

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Mock
    private UsageLoggingUtil usageLoggingUtil;

    @Mock
    private ClusterService clusterService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private StackUpdater underTest;

    @Mock
    private StackBase stackBase;

    @Mock
    private ClusterBase clusterBase;

    @Before
    public void setUp() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<StackClusterStatusView>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
        when(stackService.getStackBaseById(1L)).thenReturn(stackBase);
    }

    @Test
    public void skipStackStatusUpdateWhenActualStatusEqualsNewStatus() {
        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackBase.getStackStatus()).thenReturn(new StackStatus<>(stackBase, DetailedStackStatus.AVAILABLE));

        underTest.updateStackStatus(1L, newStatus, "newReason");

        verify(stackService, never()).update(any());
        verify(clusterService, never()).update(any());
        verify(usageLoggingUtil, never()).logClusterStatusChangeUsageEvent(any(), any(), any());
    }

    @Test
    public void skipStackStatusUpdateWhenStatusIsDeleteCompleted() {
        DetailedStackStatus newStatus = DetailedStackStatus.AVAILABLE;
        when(stackBase.getStackStatus()).thenReturn(new StackStatus<>(stackBase, DetailedStackStatus.DELETE_COMPLETED));

        underTest.updateStackStatus(1L, newStatus, "newReason");

        verify(stackService, never()).update(any());
        verify(clusterService, never()).update(any());
        verify(usageLoggingUtil, never()).logClusterStatusChangeUsageEvent(any(), any(), any());
    }

    @Test
    public void updateStackStatusAndReason() {
        DetailedStackStatus newStatus = DetailedStackStatus.DELETE_COMPLETED;
        String newStatusReason = "test";
        when(stackBase.getStackStatus()).thenReturn(new StackStatus<>(stackBase, DetailedStackStatus.AVAILABLE));
        when(stackBase.getCluster()).thenReturn(clusterBase);
        when(clusterBase.getId()).thenReturn(4L);
        doAnswer(invocation -> {
            StackBase stackBase = invocation.getArgument(0);
            stackBase.getStackStatus().setId(2L);
            return stackBase;
        }).when(stackService).update(any());

        underTest.updateStackStatus(1L, newStatus, newStatusReason);

        verify(stackService).update(stackBase);
        verify(clusterService, times(1)).update(clusterBase);
        verify(usageLoggingUtil, times(1)).logClusterStatusChangeUsageEvent(eq(Status.AVAILABLE), eq(Status.DELETE_COMPLETED), eq(stackBase));
    }
}