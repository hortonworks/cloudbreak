package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.RequestProcessingStepMapper;

class CDPStructuredFlowEventToCDPOperationDetailsConverterTest {

    private CDPStructuredFlowEventToCDPOperationDetailsConverter underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(underTest, "appVersion", "version-1234");
        Whitebox.setInternalState(underTest, "requestProcessingStepMapper", new RequestProcessingStepMapper());
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithNullOperation() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals("", details.getAccountId());
        Assert.assertEquals("", details.getResourceCrn());
        Assert.assertEquals("", details.getResourceName());
        Assert.assertEquals("", details.getInitiatorCrn());

        Assert.assertEquals("version-1234", details.getApplicationVersion());
    }

    @Test
    public void testInitProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("INIT_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, details.getCdpRequestProcessingStep());
    }

    @Test
    public void testFinalProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState("FINAL_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
    }

    @Test
    public void testSomethingElseProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("SOMETHING_ELSE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
    }

}