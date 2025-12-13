package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_BRINGUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_BRINGUP_FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@ExtendWith(MockitoExtension.class)
class DatalakeRecoveryBringupServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final long STACK_ID = 1L;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private DatalakeRecoveryBringupService underTest;

    @Test
    void testBringupFinished() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleDatalakeRecoveryBringupSuccess(STACK_ID);
        verifyNoInteractions(clusterService);
        verifyNoInteractions(stackUpdater);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.AVAILABLE.name()), captor.capture());
        assertEquals(DATALAKE_RECOVERY_BRINGUP_FINISHED, captor.getValue());
    }

    @Test
    void testBringupFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleDatalakeRecoveryBringupFailure(STACK_ID, ERROR_MESSAGE, CLUSTER_RECOVERY_FAILED);
        verify(stackUpdater, times(1)).updateStackStatus(STACK_ID, CLUSTER_RECOVERY_FAILED, ERROR_MESSAGE);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(DATALAKE_RECOVERY_BRINGUP_FAILED, captor.getValue());
    }
}