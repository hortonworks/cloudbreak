package com.sequenceiq.datalake.service.sdx.flowwait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.upgrade.SdxUpgradeValidationResultProvider;

@ExtendWith(MockitoExtension.class)
public class SdxWaitTaskForCloudbreakFlowTest {

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxUpgradeValidationResultProvider cloudbreakFlowResultProvider;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private PollingConfig pollingConfig;

    @InjectMocks
    private SdxWaitTaskForCloudbreakFlow sdxWaitTaskForCloudbreakFlow;

    @Test
    void testFlowIsRunningThenContinue() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(CloudbreakFlowService.FlowState.RUNNING);

        AttemptResult<Boolean> attemptResult = sdxWaitTaskForCloudbreakFlow.process();

        assertEquals(AttemptState.CONTINUE, attemptResult.getState());
    }

    @Test
    void testFlowIsFinishedWithSuccessThenFinish() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(CloudbreakFlowService.FlowState.FINISHED);
        when(cloudbreakFlowService.getFlowResultByFlowId(sdxCluster)).thenReturn(true);

        AttemptResult<Boolean> attemptResult = sdxWaitTaskForCloudbreakFlow.process();

        assertEquals(AttemptState.FINISH, attemptResult.getState());
        assertTrue(attemptResult.getResult());
    }

    @Test
    void testFlowIsFinishedWithErrorThenBreak() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(CloudbreakFlowService.FlowState.FINISHED);
        when(cloudbreakFlowService.getFlowResultByFlowId(sdxCluster)).thenReturn(false);

        AttemptResult<Boolean> attemptResult = sdxWaitTaskForCloudbreakFlow.process();

        assertEquals(AttemptState.BREAK, attemptResult.getState());
    }

    @Test
    void testFlowIsUnknownThenBreak() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(CloudbreakFlowService.FlowState.UNKNOWN);

        AttemptResult<Boolean> attemptResult = sdxWaitTaskForCloudbreakFlow.process();

        assertEquals(AttemptState.BREAK, attemptResult.getState());
    }

}
