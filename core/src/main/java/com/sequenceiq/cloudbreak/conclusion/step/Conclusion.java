package com.sequenceiq.cloudbreak.conclusion.step;

public class Conclusion {

    private final Class<? extends ConclusionStep> conclusionStepClass;

    private final boolean failureFound;

    private final String conclusion;

    private final String details;

    public Conclusion(boolean failureFound, String conclusion, String details, Class<? extends ConclusionStep> conclusionStepClass) {
        this.failureFound = failureFound;
        this.conclusion = conclusion;
        this.details = details;
        this.conclusionStepClass = conclusionStepClass;
    }

    public static Conclusion succeeded(Class<? extends ConclusionStep> conclusionStepClass) {
        return new Conclusion(false, null, null, conclusionStepClass);
    }

    public static Conclusion failed(String conclusion, String details, Class<? extends ConclusionStep> conclusionStepClass) {
        return new Conclusion(true, conclusion, details, conclusionStepClass);
    }

    public boolean isFailureFound() {
        return failureFound;
    }

    public String getConclusion() {
        return conclusion;
    }

    public String getDetails() {
        return details;
    }

    public Class<? extends ConclusionStep> getConclusionStepClass() {
        return conclusionStepClass;
    }

    @Override
    public String toString() {
        return "Conclusion{" +
                "step='" + conclusionStepClass.getSimpleName() + '\'' +
                ", failureFound=" + failureFound +
                ", conclusion='" + conclusion + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
