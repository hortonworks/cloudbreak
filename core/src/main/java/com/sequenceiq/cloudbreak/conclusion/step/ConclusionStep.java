package com.sequenceiq.cloudbreak.conclusion.step;

public abstract class ConclusionStep {

    private final Class<? extends ConclusionStep> conclusionStepClass;

    public ConclusionStep() {
        this.conclusionStepClass = getClass();
    }

    public abstract Conclusion check(Long resourceId);

    public Conclusion succeeded() {
        return Conclusion.succeeded(conclusionStepClass);
    }

    public Conclusion failed(String conclusion, String details) {
        return Conclusion.failed(conclusion, details, conclusionStepClass);
    }

}
