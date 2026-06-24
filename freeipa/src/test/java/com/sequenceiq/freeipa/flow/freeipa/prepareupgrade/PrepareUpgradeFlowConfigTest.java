package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_RESULT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeState.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class PrepareUpgradeFlowConfigTest {

    private final PrepareUpgradeFlowConfig underTest = new PrepareUpgradeFlowConfig();

    @Test
    void awsSgValidationTransitionUsesHandlerFailureEvent() {
        Transition<PrepareUpgradeState, PrepareUpgradeEvent> transition = underTest.getTransitions().stream()
                .filter(t -> t.getSource() == PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_STATE
                        && t.getEvent() == PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Prepare upgrade SG validation transition is missing"));
        assertEquals(PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_RESULT_STATE, transition.getTarget());
        assertEquals(PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FAILED_EVENT, transition.getFailureEvent());
    }
}
