package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

class FreeIpaUseCaseMapperTest {

    private static final String UPSCALEFLOWCONFIG = "UpscaleFlowConfig";

    private static final String OTHERFLOWEVENTCHAINFACTORY = "OtherFlowEventChainFactory";

    private static final String UPSCALE_FINISHED_STATE = "UPSCALE_FINISHED_STATE";

    private static final String SOMETHING_FAILED_STATE = "SOMETHING_FAILED_STATE";

    private static final String INIT_STATE = "INIT_STATE";

    private FreeIpaUseCaseMapper underTest;

    @BeforeEach()
    void setUp() {
        underTest = new FreeIpaUseCaseMapper();
        ReflectionTestUtils.setField(underTest, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        underTest.initUseCaseMaps();
    }

    @Test
    void testNullFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET, underTest.useCase(null));
    }

    @Test
    void testEmptyFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    void testOtherNextFlowStateMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, "SOME_STATE"));
    }

    @Test
    void testInitNextFlowStateWithCorrectFlowChainAndFlowTypeMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.CREATE_STARTED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "StackProvisionFlowConfig", INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.DELETE_STARTED,
                mapFlowDetailsToUseCase(null, "StackTerminationFlowConfig", INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.SUSPEND_STARTED,
                mapFlowDetailsToUseCase(null, "StackStopFlowConfig", INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.RESUME_STARTED,
                mapFlowDetailsToUseCase(null, "StackStartFlowConfig", INIT_STATE));

    }

    @Test
    void testInitNextFlowStateWithIncorrectFlowChainAndFlowTypeMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(OTHERFLOWEVENTCHAINFACTORY, UPSCALEFLOWCONFIG, INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(OTHERFLOWEVENTCHAINFACTORY, "OtherFlowConfig", INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(OTHERFLOWEVENTCHAINFACTORY, null, INIT_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, INIT_STATE));
    }

    @Test
    void testCorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FINISHED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, UPSCALE_FINISHED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FAILED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, SOMETHING_FAILED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FAILED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, "SOME_FAIL_STATE"));

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.CREATE_FINISHED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "FreeIpaProvisionFlowConfig", "FREEIPA_PROVISION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "StackProvisionFlowConfig", SOMETHING_FAILED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.CREATE_FAILED,
                mapFlowDetailsToUseCase("ProvisionFlowEventChainFactory", "FreeIpaProvisionFlowConfig", SOMETHING_FAILED_STATE));

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.DELETE_FINISHED,
                mapFlowDetailsToUseCase(null, "StackTerminationFlowConfig", "TERMINATION_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.DELETE_FAILED,
                mapFlowDetailsToUseCase(null, "StackTerminationFlowConfig", SOMETHING_FAILED_STATE));

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.SUSPEND_FINISHED,
                mapFlowDetailsToUseCase(null, "StackStopFlowConfig", "STOP_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.SUSPEND_FAILED,
                mapFlowDetailsToUseCase(null, "StackStopFlowConfig", SOMETHING_FAILED_STATE));

        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.RESUME_FINISHED,
                mapFlowDetailsToUseCase(null, "StackStartFlowConfig", "START_FINISHED_STATE"));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.RESUME_FAILED,
                mapFlowDetailsToUseCase(null, "StackStartFlowConfig", SOMETHING_FAILED_STATE));
    }

    @Test
    void testCorrectFinishedAndFailedNextFlowStatesWithIncorrectFlowChainMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(OTHERFLOWEVENTCHAINFACTORY, UPSCALEFLOWCONFIG, UPSCALE_FINISHED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, UPSCALE_FINISHED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(OTHERFLOWEVENTCHAINFACTORY, null, SOMETHING_FAILED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, null, SOMETHING_FAILED_STATE));
    }

    @Test
    void testIncorrectFinishedNextFlowStatesWithCorrectFlowMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, "OTHER_FINISHED_STATE"));
    }

    private UsageProto.CDPFreeIPAStatus.Value mapFlowDetailsToUseCase(String flowChainType, String flowType, String nextFlowState) {
        FlowDetails flowDetails = new FlowDetails();
        flowDetails.setFlowChainType(flowChainType);
        flowDetails.setFlowType(flowType);
        flowDetails.setNextFlowState(nextFlowState);

        return underTest.useCase(flowDetails);
    }
}
