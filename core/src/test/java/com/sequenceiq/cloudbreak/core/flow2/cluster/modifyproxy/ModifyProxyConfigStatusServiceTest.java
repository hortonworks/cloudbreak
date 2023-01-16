package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_ON_CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_SALT_STATE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MODIFY_PROXY_CONFIG_SUCCESS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigStatusServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ModifyProxyConfigStatusService underTest;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Test
    void applyingSaltState() {
        underTest.applyingSaltState(STACK_ID);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.MODIFY_PROXY_CONFIG_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_SALT_STATE);
    }

    @Test
    void updateClusterManager() {
        underTest.updateClusterManager(STACK_ID);

        verifyNoInteractions(stackUpdater);
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_ON_CM);
    }

    @Test
    void success() {
        underTest.success(STACK_ID);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.AVAILABLE);
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_IN_PROGRESS.name(), CLUSTER_MODIFY_PROXY_CONFIG_SUCCESS);
    }

    @Test
    void failed() {
        String message = "cause";
        Exception cause = new Exception(message);

        underTest.failed(STACK_ID, cause);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.SALT_UPDATE_FAILED, message);
        verify(flowMessageService).fireEventAndLog(STACK_ID, UPDATE_FAILED.name(), CLUSTER_MODIFY_PROXY_CONFIG_FAILED, message);
    }

}
