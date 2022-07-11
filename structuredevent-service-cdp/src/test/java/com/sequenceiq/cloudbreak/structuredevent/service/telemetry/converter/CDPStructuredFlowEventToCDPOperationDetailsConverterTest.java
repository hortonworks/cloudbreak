package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

class CDPStructuredFlowEventToCDPOperationDetailsConverterTest {

    private CDPStructuredFlowEventToCDPOperationDetailsConverter underTest;

    @BeforeEach()
    void setUp() {
        underTest = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
        ReflectionTestUtils.setField(underTest, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPOperationDetails details = underTest.convert(null, null);

        assertEquals("", details.getAccountId());
        assertEquals("", details.getResourceCrn());
        assertEquals("", details.getResourceName());
        assertEquals("", details.getInitiatorCrn());
        assertEquals("", details.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowId());
        assertEquals("", details.getFlowChainId());
        assertEquals("", details.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, details.getEnvironmentType());

        assertEquals("version-1234", details.getApplicationVersion());
    }

    @Test
    void testConversionWithNullOperation() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals("", details.getAccountId());
        assertEquals("", details.getResourceCrn());
        assertEquals("", details.getResourceName());
        assertEquals("", details.getInitiatorCrn());
        assertEquals("", details.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowId());
        assertEquals("", details.getFlowChainId());
        assertEquals("", details.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, details.getEnvironmentType());

        assertEquals("version-1234", details.getApplicationVersion());
    }

    @Test
    void testEnvironmentTypeConversion() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, "AWS");
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, details.getEnvironmentType());

        assertThrows(IllegalArgumentException.class, () -> underTest.convert(cdpStructuredFlowEvent, "SOMETHING"));
    }

    @Test
    void testInitProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("INIT_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testFinalProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("ENV_CREATION_FAILED_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());

        flowDetails.setNextFlowState("ENV_CREATION_FINISHED_STATE");

        details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testSomethingElseProcessingType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState("SOMETHING_ELSE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
    }

    @Test
    void testFlowAndFlowChainType() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowId("flowId");
        flowDetails.setFlowChainId("flowChainId");
        flowDetails.setNextFlowState("ENV_CREATION_FINISHED_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        CDPOperationDetails operationDetails = new CDPOperationDetails();
        operationDetails.setUuid("correlationId");
        cdpStructuredFlowEvent.setOperation(operationDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("flowId", details.getFlowId());
        assertEquals("flowChainId", details.getFlowChainId());
        assertEquals("correlationId", details.getCorrelationId());
    }

    @Test
    void testNoFlowChain() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowId("flowId");
        flowDetails.setFlowState("SOMETHING");
        flowDetails.setNextFlowState("ENV_CREATION_FINISHED_STATE");
        cdpStructuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("flowId", details.getFlowId());
        assertEquals("flowId", details.getFlowChainId());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testFlowStateOnlyFilledOutInCaseOfFailure() {
        CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent = new CDPEnvironmentStructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        cdpStructuredFlowEvent.setFlow(flowDetails);

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("ENV_CREATION_FAILED_STATE");
        UsageProto.CDPOperationDetails details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("FLOW_STATE", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("DOWNSCALE_FAIL_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("FLOW_STATE", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("ENV_CREATION_FINISHED_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("INIT_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState(null);
        flowDetails.setNextFlowState("ENV_CREATION_FAILED_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState(null);
        flowDetails.setNextFlowState("DOWNSCALE_FAIL_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("ENV_CREATION_FAILED_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("DOWNSCALE_FAIL_STATE");
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState(null);
        details = underTest.convert(cdpStructuredFlowEvent, null);
        assertEquals("", details.getFlowState());
    }
}
