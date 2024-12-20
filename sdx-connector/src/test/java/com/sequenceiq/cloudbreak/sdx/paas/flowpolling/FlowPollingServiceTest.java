package com.sequenceiq.cloudbreak.sdx.paas.flowpolling;

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
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.endpoint.SdxFlowEndpoint;

@ExtendWith(MockitoExtension.class)
class FlowPollingServiceTest {
    private static final String FLOW_POLL_ID = "poll-id";

    private static final String FLOW_ERROR_MSG_ROOT = "SDX flow operation failed for flow ID:";

    @Mock
    private SdxFlowEndpoint sdxFlowEndpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private FlowPollingService flowPollingService;

    @Test
    void finalizedSuccessfulFlowFinishesPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(false);
        flowCheckResponse.setHasActiveFlow(false);
        when(sdxFlowEndpoint.hasFlowRunningByFlowId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void ongoingFlowContinuesPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(false);
        flowCheckResponse.setHasActiveFlow(true);
        when(sdxFlowEndpoint.hasFlowRunningByFlowId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void failedFlowFailsPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(true);
        flowCheckResponse.setHasActiveFlow(false);
        when(sdxFlowEndpoint.hasFlowRunningByFlowId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.BREAK, result.getState());
        assertTrue(result.getMessage().contains(FLOW_ERROR_MSG_ROOT));
    }

    @Test
    void finalizedSuccessfulFlowChainFinishesPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(false);
        flowCheckResponse.setHasActiveFlow(false);
        when(sdxFlowEndpoint.hasFlowRunningByChainId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.FINISH, result.getState());
    }

    @Test
    void ongoingFlowChainContinuesPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(false);
        flowCheckResponse.setHasActiveFlow(true);
        when(sdxFlowEndpoint.hasFlowRunningByChainId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    @Test
    void failedFlowChainFailsPoll() {
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        FlowIdentifier flowId = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_POLL_ID);
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setLatestFlowFinalizedAndFailed(true);
        flowCheckResponse.setHasActiveFlow(false);
        when(sdxFlowEndpoint.hasFlowRunningByChainId(FLOW_POLL_ID)).thenReturn(flowCheckResponse);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.BREAK, result.getState());
        assertTrue(result.getMessage().contains(FLOW_ERROR_MSG_ROOT));
    }

    @Test
    void flowNotTriggeredJustFinishesPoll() {
        FlowIdentifier flowId = new FlowIdentifier(FlowType.NOT_TRIGGERED, null);
        AttemptResult<Object> result = flowPollingService.pollFlowIdAndReturnAttemptResult(flowId);

        assertEquals(AttemptState.FINISH, result.getState());
    }
}
