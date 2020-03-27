package com.sequenceiq.freeipa.service.freeipa.cleanup;

import org.junit.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupState;

public class CleanupStepToStateNameConverterTest {

    private CleanupStepToStateNameConverter underTest = new CleanupStepToStateNameConverter();

    @Test
    public void testAllCleanupStep() {
        for (CleanupStep cleanupStep : CleanupStep.values()) {
            String stateName = underTest.convert(cleanupStep);
            FreeIpaCleanupState.valueOf(stateName);
        }
    }
}