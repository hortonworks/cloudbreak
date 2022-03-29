package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

public class EnvironmentUseCaseMapperTest {

    private EnvironmentUseCaseMapper underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new EnvironmentUseCaseMapper();
        Whitebox.setInternalState(underTest, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
    }

    @Test
    public void testNullFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, underTest.useCase(null));
    }

    @Test
    public void testEmptyFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    public void testOtherNextFlowStateMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("SOME_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsWithFlowTypeToUseCase("SOME_STATE", "SomeOtherFlowConfig"));
    }

    @Test
    public void testInitNextFlowStatesMappedToStartedUseCases() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_STARTED,
                mapFlowDetailsWithFlowTypeToUseCase("INIT_STATE", "EnvCreationFlowConfig"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.DELETE_STARTED,
                mapFlowDetailsWithFlowTypeToUseCase("INIT_STATE", "EnvDeleteFlowConfig"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.RESUME_STARTED,
                mapFlowDetailsWithFlowTypeToUseCase("INIT_STATE", "EnvStartFlowConfig"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_STARTED,
                mapFlowDetailsWithFlowTypeToUseCase("INIT_STATE", "EnvStopFlowConfig"));
    }

    @Test
    public void testInitNextFlowStateWithIncorrectFlowTypeMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("INIT_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsWithFlowTypeToUseCase("INIT_STATE", "SomeOtherFlowConfig"));
    }

    @Test
    public void testCorrectFinishedAndFailedNextFlowStatesMappedCorrectly() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_FINISHED,
                mapFlowDetailsToUseCase("ENV_CREATION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ENV_CREATION_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.DELETE_FINISHED,
                mapFlowDetailsToUseCase("ENV_DELETE_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.DELETE_FAILED,
                mapFlowDetailsToUseCase("ENV_DELETE_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.RESUME_FINISHED,
                mapFlowDetailsToUseCase("ENV_START_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.RESUME_FAILED,
                mapFlowDetailsToUseCase("ENV_START_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FINISHED,
                mapFlowDetailsToUseCase("ENV_STOP_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.SUSPEND_FAILED,
                mapFlowDetailsToUseCase("ENV_STOP_FAILED_STATE"));
    }

    @Test
    public void testOtherFinishedAndFailedNextFlowStatesMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("SOME_OTHER_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("SOME_OTHER_FAILED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET,
                mapFlowDetailsToUseCase("_FAILED_STATE"));
    }

    private UsageProto.CDPEnvironmentStatus.Value mapFlowDetailsWithFlowTypeToUseCase(String nextFlowState, String flowType) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState(nextFlowState);
        flowDetails.setFlowType(flowType);

        return underTest.useCase(flowDetails);
    }

    private UsageProto.CDPEnvironmentStatus.Value mapFlowDetailsToUseCase(String nextFlowState) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setNextFlowState(nextFlowState);

        return underTest.useCase(flowDetails);
    }
}
