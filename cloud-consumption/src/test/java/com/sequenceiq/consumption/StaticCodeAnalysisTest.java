package com.sequenceiq.consumption;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.util.ConstraintValidationModificationChecker;
import com.sequenceiq.cloudbreak.util.UnusedInjectChecker;

public class StaticCodeAnalysisTest {

    @Test
    public void testIfThereAreUnusedInjections() {
        new UnusedInjectChecker().check();
    }

    @Test
    public void testIfThereAreCustomConstraintValidationModifications() {
        new ConstraintValidationModificationChecker().check();
    }
}
