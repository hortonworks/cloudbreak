package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

public class FreeIpaUseCaseMapperTest {

    private static final String UPSCALEFLOWCONFIG = "UpscaleFlowConfig";

    private static final String OTHERFLOWEVENTCHAINFACTORY = "OtherFlowEventChainFactory";

    private static final String UPSCALE_FINISHED_STATE = "UPSCALE_FINISHED_STATE";

    private static final String SOMETHING_FAILED_STATE = "SOMETHING_FAILED_STATE";

    private static final String INIT_STATE = "INIT_STATE";

    private FreeIpaUseCaseMapper underTest;

    @BeforeEach()
    public void setUp() {
        underTest = new FreeIpaUseCaseMapper();
        Whitebox.setInternalState(underTest, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        underTest.initUseCaseMaps();
    }

    @Test
    public void testNullFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET, underTest.useCase(null));
    }

    @Test
    public void testEmptyFlowDetailsMappedToUnset() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET, underTest.useCase(new FlowDetails()));
    }

    @Test
    public void testOtherNextFlowStateMappedToUnsetUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UNSET,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, "SOME_STATE"));
    }

    @Test
    public void testInitNextFlowStateWithCorrectFlowChainAndFlowTypeMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_STARTED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, INIT_STATE));
    }

    @Test
    public void testInitNextFlowStateWithIncorrectFlowChainAndFlowTypeMappedToUnsetUseCase() {
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
    public void testCorrectFinishedAndFailedNextFlowStatesWithCorrectFlowChainMappedToCorrectUseCase() {
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FINISHED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, UPSCALE_FINISHED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FAILED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, SOMETHING_FAILED_STATE));
        Assertions.assertEquals(UsageProto.CDPFreeIPAStatus.Value.UPSCALE_FAILED,
                mapFlowDetailsToUseCase(null, UPSCALEFLOWCONFIG, "SOME_FAIL_STATE"));
    }

    @Test
    public void testCorrectFinishedAndFailedNextFlowStatesWithIncorrectFlowChainMappedToUnsetUseCase() {
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
    public void testIncorrectFinishedNextFlowStatesWithCorrectFlowMappedToUnsetUseCase() {
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
