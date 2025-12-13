package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

public class CDPRequestProcessingStepMapperTest {

    private CDPRequestProcessingStepMapper underTest = new CDPRequestProcessingStepMapper();

    @Test
    public void testInitNextFlowStateMappedCorrectlyToInit() {
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, mapNextFlowStateToProcessingStep("INIT_STATE"));
    }

    @Test
    public void testFinishedOrFailedNextFlowStateMappedCorrectlyToFinal() {
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FINISHED_STATE"));
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FAILED_STATE"));
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FAIL_STATE"));
    }

    @Test
    public void testOtherNextFlowStateMappedCorrectlyToUnset() {
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, mapNextFlowStateToProcessingStep("OTHER_STATE"));
    }

    private UsageProto.CDPRequestProcessingStep.Value mapNextFlowStateToProcessingStep(String nextFlowState) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState(nextFlowState);

        return underTest.mapIt(flowDetails);
    }
}
