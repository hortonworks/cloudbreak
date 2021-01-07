package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.RequestProcessingStepMapper;

public class StructuredFlowEventToCDPOperationDetailsConverterTest {

    private StructuredFlowEventToCDPOperationDetailsConverter underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new StructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(underTest, "appVersion", "version-1234");
        Whitebox.setInternalState(underTest, "requestProcessingStepMapper", new RequestProcessingStepMapper());
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithNullOperation() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", details.getAccountId());
        Assert.assertEquals("", details.getResourceCrn());
        Assert.assertEquals("", details.getResourceName());
        Assert.assertEquals("", details.getInitiatorCrn());

        Assert.assertEquals("version-1234", details.getApplicationVersion());
    }

    @Test
    public void testInitProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("INIT_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, details.getCdpRequestProcessingStep());
    }

    @Test
    public void testFinalProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState("FINAL_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
    }

    @Test
    public void testSomethingElseProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("SOMETHING_ELSE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
    }

    @Test
    public void testFlowAndFlowChainType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowId("flowId");
        flowDetails.setFlowChainId("flowChainId");
        flowDetails.setNextFlowState("FINAL_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        Assert.assertEquals("flowId", details.getFlowId());
        Assert.assertEquals("flowChainId", details.getFlowChainId());
    }
}
