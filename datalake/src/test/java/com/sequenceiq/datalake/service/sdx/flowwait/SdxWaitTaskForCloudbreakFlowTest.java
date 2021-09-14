package com.sequenceiq.datalake.service.sdx.flowwait;

import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FAILED;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.RUNNING;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowCheckResponseToFlowStateConverter;
import com.sequenceiq.datalake.service.sdx.flowwait.task.SdxWaitTaskForCloudbreakFlow;

@ExtendWith(MockitoExtension.class)
public class SdxWaitTaskForCloudbreakFlowTest {

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxCluster sdxCluster;

    @Mock
    private FlowCheckResponseToFlowStateConverter flowCheckResponseToFlowStateConverter;

    @InjectMocks
    private SdxWaitTaskForCloudbreakFlow underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "cloudbreakFlowService", cloudbreakFlowService);
    }

    @Test
    void testFlowIsRunningThenContinue() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(RUNNING);

        AttemptResult<Boolean> attemptResult = underTest.process();

        assertEquals(AttemptState.CONTINUE, attemptResult.getState());
    }

    @Test
    void testFlowIsFinishedWithSuccessThenFinish() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FINISHED);

        AttemptResult<Boolean> attemptResult = underTest.process();

        assertEquals(AttemptState.FINISH, attemptResult.getState());
        assertTrue(attemptResult.getResult());
    }

    @Test
    void testFlowIsFinishedWithFailedThenBreak() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(FAILED);

        AttemptResult<Boolean> attemptResult = underTest.process();

        assertEquals(AttemptState.BREAK, attemptResult.getState());
    }

    @Test
    void testFlowIsUnknownThenBreak() throws Exception {
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster)).thenReturn(UNKNOWN);

        AttemptResult<Boolean> attemptResult = underTest.process();

        assertEquals(AttemptState.BREAK, attemptResult.getState());
    }

}
