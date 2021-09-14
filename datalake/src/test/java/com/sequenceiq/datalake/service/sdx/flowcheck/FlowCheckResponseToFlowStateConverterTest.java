package com.sequenceiq.datalake.service.sdx.flowcheck;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.api.model.FlowCheckResponse;

public class FlowCheckResponseToFlowStateConverterTest {

    private final FlowCheckResponseToFlowStateConverter underTest = new FlowCheckResponseToFlowStateConverter();

    @Test
    void convertRunning() {
        assertEquals(FlowState.RUNNING, underTest.convert(getFlowCheckResponse(TRUE, null)));
        assertEquals(FlowState.RUNNING, underTest.convert(getFlowCheckResponse(TRUE, TRUE)));
        assertEquals(FlowState.RUNNING, underTest.convert(getFlowCheckResponse(TRUE, FALSE)));
    }

    @Test
    void convertFinished() {
        assertEquals(FlowState.FINISHED, underTest.convert(getFlowCheckResponse(FALSE, null)));
        assertEquals(FlowState.FAILED, underTest.convert(getFlowCheckResponse(FALSE, TRUE)));
        assertEquals(FlowState.FINISHED, underTest.convert(getFlowCheckResponse(FALSE, FALSE)));
    }

    private FlowCheckResponse getFlowCheckResponse(Boolean hasActiveFlow, Boolean failed) {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(hasActiveFlow);
        flowCheckResponse.setLatestFlowFinalizedAndFailed(failed);
        return flowCheckResponse;
    }
}
