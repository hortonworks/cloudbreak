package com.sequenceiq.cloudbreak.conclusion.step;

public class ConclusionStepResult {

    private final boolean stepFailed;

    private final String conclusion;

    public ConclusionStepResult(boolean stepFailed, String conclusion) {
        this.stepFailed = stepFailed;
        this.conclusion = conclusion;
    }

    public static ConclusionStepResult succeeded() {
        return new ConclusionStepResult(false, null);
    }

    public static ConclusionStepResult failed(String conclusion) {
        return new ConclusionStepResult(true, conclusion);
    }

    public boolean isStepFailed() {
        return stepFailed;
    }

    public String getConclusion() {
        return conclusion;
    }
}
