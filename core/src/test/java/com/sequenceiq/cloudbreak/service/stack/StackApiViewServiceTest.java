package com.sequenceiq.cloudbreak.service.stack;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

@RunWith(MockitoJUnitRunner.class)
public class StackApiViewServiceTest {

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private StackApiViewService stackApiViewService;

    @Test
    public void testCanChangeCredentialWithNullStatus() {
        StackApiView stackApiView = new StackApiView();
        stackApiView.setStackStatus(null);

        boolean result = stackApiViewService.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    public void testCanChangeCredentialWithStatusIsNotAvailable() {
        StackApiView stackApiView = new StackApiView();
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        stackApiView.setStackStatus(stackStatusView);

        boolean result = stackApiViewService.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    public void testCanChangeCredentialWithOngoingFlowOperation() {
        Long stackId = 1L;
        StackApiView stackApiView = new StackApiView();
        stackApiView.setId(stackId);
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.AVAILABLE);
        stackApiView.setStackStatus(stackStatusView);
        when(flowLogService.isOtherFlowRunning(stackId)).thenReturn(true);

        boolean result = stackApiViewService.canChangeCredential(stackApiView);

        assertFalse(result);
    }

    @Test
    public void testCanChangeCredentialHappyPath() {
        Long stackId = 1L;
        StackApiView stackApiView = new StackApiView();
        stackApiView.setId(stackId);
        StackStatusView stackStatusView = Mockito.mock(StackStatusView.class);
        when(stackStatusView.getStatus()).thenReturn(Status.AVAILABLE);
        stackApiView.setStackStatus(stackStatusView);
        when(flowLogService.isOtherFlowRunning(stackId)).thenReturn(false);

        boolean result = stackApiViewService.canChangeCredential(stackApiView);

        assertTrue(result);
    }
}