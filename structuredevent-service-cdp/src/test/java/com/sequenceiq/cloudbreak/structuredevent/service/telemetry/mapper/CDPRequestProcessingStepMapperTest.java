package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

public class CDPRequestProcessingStepMapperTest {

    private CDPRequestProcessingStepMapper underTest = new CDPRequestProcessingStepMapper();

    @Test
    public void testInitNextFlowStateMappedCorrectlyToInit() {
        Assertions.assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, mapNextFlowStateToProcessingStep("INIT_STATE"));
    }

    @Test
    public void testFinishedOrFailedNextFlowStateMappedCorrectlyToFinal() {
        Assertions.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, mapNextFlowStateToProcessingStep("SOMETHING_FAIL_STATE"));
    }

    @Test
    public void testOtherNextFlowStateMappedCorrectlyToUnset() {
        Assertions.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, mapNextFlowStateToProcessingStep("OTHER_STATE"));
    }

    private UsageProto.CDPRequestProcessingStep.Value mapNextFlowStateToProcessingStep(String nextFlowState) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState(nextFlowState);

        return underTest.mapIt(flowDetails);
    }
}
