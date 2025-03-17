package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CALLER_ID_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class StructuredEventToCDPOperationDetailsConverterTest {

    private StructuredEventToCDPOperationDetailsConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(underTest, "appVersion", "version-1234");
        ReflectionTestUtils.setField(underTest, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert((StructuredFlowEvent) null);

        assertEquals("", flowOperationDetails.getAccountId());
        assertEquals("", flowOperationDetails.getResourceCrn());
        assertEquals("", flowOperationDetails.getResourceName());
        assertEquals("", flowOperationDetails.getInitiatorCrn());
        assertEquals("", flowOperationDetails.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, flowOperationDetails.getCdpRequestProcessingStep());
        assertEquals("", flowOperationDetails.getFlowId());
        assertEquals("", flowOperationDetails.getFlowChainId());
        assertEquals("", flowOperationDetails.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, flowOperationDetails.getEnvironmentType());

        assertEquals("version-1234", flowOperationDetails.getApplicationVersion());

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert((StructuredSyncEvent) null);

        assertEquals("", syncOperationDetails.getAccountId());
        assertEquals("", syncOperationDetails.getResourceCrn());
        assertEquals("", syncOperationDetails.getResourceName());
        assertEquals("", syncOperationDetails.getInitiatorCrn());
        assertEquals("", syncOperationDetails.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.SYNC, syncOperationDetails.getCdpRequestProcessingStep());
        assertEquals("", syncOperationDetails.getFlowId());
        assertEquals("", syncOperationDetails.getFlowChainId());
        assertEquals("", syncOperationDetails.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, syncOperationDetails.getEnvironmentType());

        assertEquals("version-1234", syncOperationDetails.getApplicationVersion());
    }

    @Test
    void testConversionWithNullOperation() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowOperationDetails.getAccountId());
        assertEquals("", flowOperationDetails.getResourceCrn());
        assertEquals("", flowOperationDetails.getResourceName());
        assertEquals("", flowOperationDetails.getInitiatorCrn());
        assertEquals("", flowOperationDetails.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, flowOperationDetails.getCdpRequestProcessingStep());
        assertEquals("", flowOperationDetails.getFlowId());
        assertEquals("", flowOperationDetails.getFlowChainId());
        assertEquals("", flowOperationDetails.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, flowOperationDetails.getEnvironmentType());

        assertEquals("version-1234", flowOperationDetails.getApplicationVersion());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", syncOperationDetails.getAccountId());
        assertEquals("", syncOperationDetails.getResourceCrn());
        assertEquals("", syncOperationDetails.getResourceName());
        assertEquals("", syncOperationDetails.getInitiatorCrn());
        assertEquals("", syncOperationDetails.getCorrelationId());
        assertEquals(UsageProto.CDPRequestProcessingStep.Value.SYNC, syncOperationDetails.getCdpRequestProcessingStep());
        assertEquals("", syncOperationDetails.getFlowId());
        assertEquals("", syncOperationDetails.getFlowChainId());
        assertEquals("", syncOperationDetails.getFlowState());
        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, syncOperationDetails.getEnvironmentType());

        assertEquals("version-1234", syncOperationDetails.getApplicationVersion());
    }

    @Test
    void testInitProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("INIT_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.INIT, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testFinalProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());

        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");

        details = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testSomethingElseProcessingType() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState("SOMETHING_ELSE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
    }

    @Test
    void testFlowAndFlowChainType() {
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

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("flowId", details.getFlowId());
        assertEquals("flowChainId", details.getFlowChainId());
        assertEquals("correlationId", details.getCorrelationId());
    }

    @Test
    void testNoFlowChain() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowId("flowId");
        flowDetails.setFlowState("SOMETHING");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");
        structuredFlowEvent.setFlow(flowDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPRequestProcessingStep.Value.FINAL, details.getCdpRequestProcessingStep());
        assertEquals("flowId", details.getFlowId());
        assertEquals("flowId", details.getFlowChainId());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testFlowStateOnlyFilledOutInCaseOfFailure() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        FlowDetails flowDetails = new FlowDetails();
        structuredFlowEvent.setFlow(flowDetails);

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);
        assertEquals("FLOW_STATE", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FINISHED_STATE");
        details = underTest.convert(structuredFlowEvent);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState("INIT_STATE");
        details = underTest.convert(structuredFlowEvent);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState(null);
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        details = underTest.convert(structuredFlowEvent);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("unknown");
        flowDetails.setNextFlowState("CLUSTER_CREATION_FAILED_STATE");
        details = underTest.convert(structuredFlowEvent);
        assertEquals("", details.getFlowState());

        flowDetails.setFlowState("FLOW_STATE");
        flowDetails.setNextFlowState(null);
        details = underTest.convert(structuredFlowEvent);
        assertEquals("", details.getFlowState());
    }

    @Test
    void testFlowRelatedOperationDetailsFieldsReturnEmptyStringWhenConvertingStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setTenant("tenant1");
        operationDetails.setResourceCrn("crn1");
        operationDetails.setResourceName("name1");
        operationDetails.setUserCrn("crn2");
        structuredSyncEvent.setOperation(operationDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        assertEquals("tenant1", details.getAccountId());
        assertEquals("crn1", details.getResourceCrn());
        assertEquals("name1", details.getResourceName());
        assertEquals("crn2", details.getInitiatorCrn());

        assertEquals("", details.getFlowId());
        assertEquals("", details.getFlowChainId());
        assertEquals("", details.getFlowState());
    }

    @Test
    void testEnvironmentTypeSetCorrectly() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails flowStackDetails = new StackDetails();
        flowStackDetails.setCloudPlatform(CloudPlatform.AWS.name());
        structuredFlowEvent.setStack(flowStackDetails);

        UsageProto.CDPOperationDetails flowOperationDetails = underTest.convert(structuredFlowEvent);

        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, flowOperationDetails.getEnvironmentType());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails syncStackDetails = new StackDetails();
        syncStackDetails.setCloudPlatform(CloudPlatform.AWS.name());
        structuredSyncEvent.setStack(syncStackDetails);

        UsageProto.CDPOperationDetails syncOperationDetails = underTest.convert(structuredSyncEvent);

        assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, syncOperationDetails.getEnvironmentType());
    }

    @Test
    void creatorClientSetCorrectlyWhenNotEmptyForStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient("testClient");
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals("testClient", details.getCreatorClient());
    }

    @Test
    void creatorClientSetToCallerIdNotFoundWhenEmptyForStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient("");
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(CALLER_ID_NOT_FOUND, details.getCreatorClient());
    }

    @Test
    void creatorClientSetToCallerIdNotFoundWhenNullForStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient(null);
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredFlowEvent);

        assertEquals(CALLER_ID_NOT_FOUND, details.getCreatorClient());
    }

    @Test
    void creatorClientSetCorrectlyWhenNotEmptyForStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient("testClient");
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        assertEquals("testClient", details.getCreatorClient());
    }

    @Test
    void creatorClientSetToCallerIdNotFoundWhenEmptyForStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient("");
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        assertEquals(CALLER_ID_NOT_FOUND, details.getCreatorClient());
    }

    @Test
    void creatorClientSetToCallerIdNotFoundWhenNullForStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails stackDetails = new StackDetails();
        stackDetails.setCreatorClient(null);
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        assertEquals(CALLER_ID_NOT_FOUND, details.getCreatorClient());
    }

}
