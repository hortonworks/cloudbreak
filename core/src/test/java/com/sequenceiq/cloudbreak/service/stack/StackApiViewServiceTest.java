package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class StackApiViewServiceTest {

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private StackApiViewService underTest;

    @Test
    void testCanChangeCredentialWithNullStatus() {
        StackApiView stackApiView = new StackApiView();
        stackApiView.setStackStatus(null);

        boolean result = underTest.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    void testCanChangeCredentialWithStatusIsNotAvailable() {
        StackApiView stackApiView = new StackApiView();
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        stackApiView.setStackStatus(stackStatusView);

        boolean result = underTest.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    void testCanChangeCredentialWithOngoingFlowOperation() {
        Long stackId = 1L;
        StackApiView stackApiView = new StackApiView();
        stackApiView.setId(stackId);
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.AVAILABLE);
        stackApiView.setStackStatus(stackStatusView);
        when(flowLogService.isOtherNonTerminationFlowRunning(stackId)).thenReturn(true);

        boolean result = underTest.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    void testCanChangeCredentialHappyPath() {
        Long stackId = 1L;
        StackApiView stackApiView = new StackApiView();
        stackApiView.setId(stackId);
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.AVAILABLE);
        stackApiView.setStackStatus(stackStatusView);
        when(flowLogService.isOtherNonTerminationFlowRunning(stackId)).thenReturn(false);

        boolean result = underTest.canChangeCredential(stackApiView);

        assertTrue(result);
    }

}
