package com.sequenceiq.cloudbreak.rotation.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StepProgressResponse {

    private String step;

    private String phase;

    private String status;

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StepProgressResponse{" +
                "step='" + step + '\'' +
                ", phase='" + phase + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
