package com.sequenceiq.freeipa.service.freeipa.cleanup;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState;

class CleanupStepToStateNameConverterTest {

    private CleanupStepToStateNameConverter underTest = new CleanupStepToStateNameConverter();

    @Test
    void testAllCleanupStep() {
        for (CleanupStep cleanupStep : CleanupStep.values()) {
            String stateName = underTest.convert(cleanupStep);
            FreeIpaCleanupState.valueOf(stateName);
        }
    }
}