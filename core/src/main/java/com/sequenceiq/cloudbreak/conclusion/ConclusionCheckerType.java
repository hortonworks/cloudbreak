package com.sequenceiq.cloudbreak.conclusion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NodeServicesCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.SaltCheckerConclusionStep;

public enum ConclusionCheckerType {

    DEFAULT(SaltCheckerConclusionStep.class, NodeServicesCheckerConclusionStep.class);

    private List<Class<? extends ConclusionStep>> steps;

    ConclusionCheckerType(Class<? extends ConclusionStep>... steps) {
        if (steps != null) {
            this.steps = Arrays.asList(steps);
        } else {
            this.steps = Collections.emptyList();
        }
    }

    public List<Class<? extends ConclusionStep>> getSteps() {
        return steps;
    }
}
