package com.sequenceiq.cloudbreak.conclusion.step;

public interface ConclusionStep {

    ConclusionStepResult check(Long resourceId);

}
