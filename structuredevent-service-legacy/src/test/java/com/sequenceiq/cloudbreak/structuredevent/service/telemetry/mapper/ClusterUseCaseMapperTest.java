package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

class ClusterUseCaseMapperTest {

    private ClusterUseCaseMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClusterUseCaseMapper();
        ReflectionTestUtils.setField(underTest, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        underTest.initUseCaseMaps();
    }

    @Test
    void testNullFlowDetailsMappedToUnset() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, underTest.useCase(null));
    }

    @Test
    void testEmptyFlowDetailsMappedToUnset() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    void testOtherNextFlowStateMappedToUnsetUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "CloudConfigValidationFlowConfig", "SOME_STATE"));
    }

    @Test
    void testInitNextFlowStateWithCorrectFlowChainAndFlowTypeMappedToCorrectUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "CloudConfigValidationFlowConfig", "INIT_STATE"));
    }

    @Test
    void testInitNextFlowStateWithIncorrectFlowChainAndFlowTypeMappedToUnsetUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "OtherFlowConfig", "INIT_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "INIT_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", "CloudConfigValidationFlowConfig", "INIT_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, "CloudConfigValidationFlowConfig", "INIT_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", "OtherFlowConfig", "INIT_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "INIT_STATE"));
    }

    @Test
    void testCorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToCorrectUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FINISHED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "CLUSTER_CREATION_FINISHED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "SOMETHING_FAILED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "SOME_FAILURE_STATE"));
    }

    @Test
    void testCorrectFinishedAndFailedNextFlowStatesWithIncorrectFlowChainMappedToUnsetUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", null, "CLUSTER_CREATION_FINISHED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "CLUSTER_CREATION_FINISHED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", null, "SOMETHING_FAILED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "SOMETHING_FAILED_STATE"));
    }

    @Test
    void testIncorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToUnsetUseCase() {
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "OTHER_FINISHED_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("UpscaleFlowEventChainFactory", null, "FINALIZE_OTHER_STATE"));
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "OTHER_STATE"));
    }

    private UsageProto.CDPClusterStatus.Value mapFlowDetailsToUseCase(String flowChainType, String flowType, String nextFlowState) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowChainType(flowChainType);
        flowDetails.setFlowType(flowType);
        flowDetails.setNextFlowState(nextFlowState);

        return underTest.useCase(flowDetails);
    }
}
