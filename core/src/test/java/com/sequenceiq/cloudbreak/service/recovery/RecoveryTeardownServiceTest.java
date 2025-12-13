package com.sequenceiq.cloudbreak.service.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_TEARDOWN_FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class RecoveryTeardownServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final long STACK_ID = 1L;

    @Mock
    private TerminationService terminationService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakMetricService metricService;

    @Mock
    private StackTerminationContext stackTerminationContext;

    @Mock
    private Stack stack;

    @InjectMocks
    private RecoveryTeardownService underTest;

    @Test
    void testTeardownFinished() {
        TerminateStackResult terminateStackResult = new TerminateStackResult(STACK_ID);
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        when(stack.getId()).thenReturn(STACK_ID);

        underTest.handleRecoveryTeardownSuccess(stack, terminateStackResult);
        verifyNoInteractions(stackUpdater);
        verify(terminationService).finalizeRecoveryTeardown(STACK_ID);
        verify(metricService).incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_SUCCESSFUL, stack);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.DELETE_COMPLETED.name()), captor.capture());
        assertEquals(DATALAKE_RECOVERY_TEARDOWN_FINISHED, captor.getValue());
    }

    @Test
    void testTeardownFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        Exception exception = new Exception(ERROR_MESSAGE);
        String stackUpdateMessage = "Recovery failed: " + exception.getMessage();
        StackView stackView = mock(StackView.class);

        when(stackView.getId()).thenReturn(STACK_ID);

        underTest.handleRecoveryTeardownError(stackView, exception);

        verify(stackUpdater).updateStackStatus(STACK_ID, CLUSTER_RECOVERY_FAILED, stackUpdateMessage);
        verify(metricService).incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_FAILED, stackView, exception);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(CLUSTER_RECOVERY_FAILED.name()), captor.capture(), eq(stackUpdateMessage));
        assertEquals(DATALAKE_RECOVERY_FAILED, captor.getValue());
    }
}