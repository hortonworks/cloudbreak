package com.sequenceiq.environment.environment.flow.upgrade.ccm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.UpgradeCcmState;

class UpgradeCcmFlowConfigTest {

    private final UpgradeCcmFlowConfig underTest = new UpgradeCcmFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(UpgradeCcmState.class)
    void testGetUseCaseForFlowState(UpgradeCcmState flowState) {
        UsageProto.CDPEnvironmentStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (UpgradeCcmState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_STARTED, actual);
        } else if (UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FINISHED, actual);
        } else if (UpgradeCcmState.UPGRADE_CCM_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.CCM_UPGRADE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPEnvironmentStatus.Value.UNSET, actual);
        }
    }
}