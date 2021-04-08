package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

public class ClusterUseCaseMapperTest {

    @Autowired
    private ClusterUseCaseMapper underTest;

    @BeforeEach
    public void setUp() {
        underTest = new ClusterUseCaseMapper();
        Whitebox.setInternalState(underTest, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        underTest.initUseCaseMaps();
    }

    @Test
    public void testNullFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, underTest.useCase(null));
    }

    @Test
    public void testEmptyFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    public void testOtherNextFlowStateMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "CloudConfigValidationFlowConfig", "SOME_STATE"));
    }

    @Test
    public void testInitNextFlowStateWithCorrectFlowChainAndFlowTypeMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "CloudConfigValidationFlowConfig", "INIT_STATE"));
    }

    @Test
    public void testInitNextFlowStateWithIncorrectFlowChainAndFlowTypeMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "OtherFlowConfig", "INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", "CloudConfigValidationFlowConfig", "INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, "CloudConfigValidationFlowConfig", "INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", "OtherFlowConfig", "INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "INIT_STATE"));
    }

    @Test
    public void testCorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FINISHED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "CLUSTER_CREATION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "SOMETHING_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "SOME_FAILURE_STATE"));
    }

    @Test
    public void testCorrectFinishedAndFailedNextFlowStatesWithIncorrectFlowChainMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", null, "CLUSTER_CREATION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "CLUSTER_CREATION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("OtherFlowEventChainFactory", null, "SOMETHING_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, "SOMETHING_FAILED_STATE"));
    }

    @Test
    public void testIncorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", null, "OTHER_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
                mapFlowDetailsToUseCase("UpscaleFlowEventChainFactory", null, "FINALIZE_OTHER_STATE"));
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET,
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
