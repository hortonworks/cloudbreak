package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
public class StructuredEventToCDPOperationDetailsConverterTest {

    private StructuredEventToCDPOperationDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(underTest, "appVersion", "version-1234");
        Whitebox.setInternalState(underTest, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert((StructuredFlowEvent) null);

        Assert.assertEquals("", flowOperationDetails.getAccountId());
        Assert.assertEquals("", flowOperationDetails.getResourceCrn());
        Assert.assertEquals("", flowOperationDetails.getResourceName());
        Assert.assertEquals("", flowOperationDetails.getInitiatorCrn());
        Assert.assertEquals("", flowOperationDetails.getCorrelationId());
        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, flowOperationDetails.getCdpRequestProcessingStep());
        Assert.assertEquals("", flowOperationDetails.getFlowId());
        Assert.assertEquals("", flowOperationDetails.getFlowChainId());
        Assert.assertEquals("", flowOperationDetails.getFlowState());
        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, flowOperationDetails.getEnvironmentType());

        Assert.assertEquals("version-1234", flowOperationDetails.getApplicationVersion());

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert((StructuredSyncEvent) null);

        Assert.assertEquals("", syncOperationDetails.getAccountId());
        Assert.assertEquals("", syncOperationDetails.getResourceCrn());
        Assert.assertEquals("", syncOperationDetails.getResourceName());
        Assert.assertEquals("", syncOperationDetails.getInitiatorCrn());
        Assert.assertEquals("", syncOperationDetails.getCorrelationId());
        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.SYNC, syncOperationDetails.getCdpRequestProcessingStep());
        Assert.assertEquals("", syncOperationDetails.getFlowId());
        Assert.assertEquals("", syncOperationDetails.getFlowChainId());
        Assert.assertEquals("", syncOperationDetails.getFlowState());
        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, syncOperationDetails.getEnvironmentType());

        Assert.assertEquals("version-1234", syncOperationDetails.getApplicationVersion());
    }

    @Test
    public void testConversionWithNullOperation() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowOperationDetails.getAccountId());
        Assert.assertEquals("", flowOperationDetails.getResourceCrn());
        Assert.assertEquals("", flowOperationDetails.getResourceName());
        Assert.assertEquals("", flowOperationDetails.getInitiatorCrn());
        Assert.assertEquals("", flowOperationDetails.getCorrelationId());
        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, flowOperationDetails.getCdpRequestProcessingStep());
        Assert.assertEquals("", flowOperationDetails.getFlowId());
        Assert.assertEquals("", flowOperationDetails.getFlowChainId());
        Assert.assertEquals("", flowOperationDetails.getFlowState());
        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, flowOperationDetails.getEnvironmentType());

        Assert.assertEquals("version-1234", flowOperationDetails.getApplicationVersion());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncOperationDetails.getAccountId());
        Assert.assertEquals("", syncOperationDetails.getResourceCrn());
        Assert.assertEquals("", syncOperationDetails.getResourceName());
        Assert.assertEquals("", syncOperationDetails.getInitiatorCrn());
        Assert.assertEquals("", syncOperationDetails.getCorrelationId());
        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.SYNC, syncOperationDetails.getCdpRequestProcessingStep());
        Assert.assertEquals("", syncOperationDetails.getFlowId());
        Assert.assertEquals("", syncOperationDetails.getFlowChainId());
        Assert.assertEquals("", syncOperationDetails.getFlowState());
        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, syncOperationDetails.getEnvironmentType());

        Assert.assertEquals("version-1234", syncOperationDetails.getApplicationVersion());
    }

    @Test
    public void testInitProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("INIT_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, details.getCdpRequestProcessingStep());
        Assert.assertEquals("", details.getFlowState());
    }

    @Test
    public void testFinalProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        Assert.assertEquals("", details.getFlowState());

        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");

        details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        Assert.assertEquals("", details.getFlowState());
    }

    @Test
    public void testSomethingElseProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState("SOMETHING_ELSE");
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
        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setUuid("correlationId");
        structuredFlowEvent.setOperation(operationDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        Assert.assertEquals("flowId", details.getFlowId());
        Assert.assertEquals("flowChainId", details.getFlowChainId());
        Assert.assertEquals("correlationId", details.getCorrelationId());
    }

    @Test
    public void testNoFlowChain() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowId("flowId");
        flowDetails.setFlowState("SOMETHING");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        Assert.assertEquals("flowId", details.getFlowId());
        Assert.assertEquals("flowId", details.getFlowChainId());
        Assert.assertEquals("", details.getFlowState());
    }

    @Test
    public void testFlowStateOnlyFilledOutInCaseOfFailure() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        structuredFlowEvent.setFlow(flowDetails);

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("FLOW_STATE", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");
        details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("INIT_STATE");
        details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("", details.getFlowState());

        flowDetails.setFlowState(null);
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("", details.getFlowState());

        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState(null);
        details = underTest.convert(structuredFlowEvent);
        Assertions.assertEquals("", details.getFlowState());
    }

    @Test
    public void testFlowRelatedOperationDetailsFieldsReturnEmptyStringWhenConvertingStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setTenant("tenant1");
        operationDetails.setResourceCrn("crn1");
        operationDetails.setResourceName("name1");
        operationDetails.setUserCrn("crn2");
        structuredSyncEvent.setOperation(operationDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("tenant1", details.getAccountId());
        Assert.assertEquals("crn1", details.getResourceCrn());
        Assert.assertEquals("name1", details.getResourceName());
        Assert.assertEquals("crn2", details.getInitiatorCrn());

        Assert.assertEquals("", details.getFlowId());
        Assert.assertEquals("", details.getFlowChainId());
        Assert.assertEquals("", details.getFlowState());
    }

    @Test
    public void testEnvironmentTypeSetCorrectly() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails flowStackDetails = new StackDetails();
        flowStackDetails.setCloudPlatform(CloudPlatform.AWS.name());
        structuredFlowEvent.setStack(flowStackDetails);

        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, flowOperationDetails.getEnvironmentType());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails syncStackDetails = new StackDetails();
        syncStackDetails.setCloudPlatform(CloudPlatform.AWS.name());
        structuredSyncEvent.setStack(syncStackDetails);

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, syncOperationDetails.getEnvironmentType());
    }
}
