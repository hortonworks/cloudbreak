package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@ExtendWith(MockitoExtension.class)
class RotateRdsCertificateServiceTest {

    private static final Long STACK_ID = 123L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private RotateRdsCertificateService underTest;

    @Test
    void testCheckPrerequisitesState() {
        underTest.checkPrerequisitesState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES);
    }

    @Test
    void testGetLatestRdsCertificateState() {
        underTest.getLatestRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_GET_LATEST);
    }

    @Test
    void testUpdateLatestRdsCertificateState() {
        underTest.updateLatestRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_PUSH_LATEST);
    }

    @Test
    void testRestartCmState() {
        underTest.restartCmState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_CM_RESTART);
    }

    @Test
    void testRollingRestartRdsCertificateState() {
        underTest.rollingRestartRdsCertificateState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ROLLING_SERVICE_RESTART);
    }

    @Test
    void testRotateOnProviderState() {
        underTest.rotateOnProviderState(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER);
    }

    @Test
    void testRotateRdsCertFinished() {
        underTest.rotateRdsCertFinished(STACK_ID);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), any(DetailedStackStatus.class), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FINISHED);
    }

    @Test
    void testRotateRdsCertFailed() {
        RotateRdsCertificateFailedEvent failedEvent = new RotateRdsCertificateFailedEvent(STACK_ID, new RuntimeException("error"));
        underTest.rotateRdsCertFailed(failedEvent);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.ROTATE_RDS_CERTIFICATE_FAILED), anyString());
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_FAILED.name(), ResourceEvent.ROTATE_RDS_CERTIFICATE_FAILED);
    }

}
