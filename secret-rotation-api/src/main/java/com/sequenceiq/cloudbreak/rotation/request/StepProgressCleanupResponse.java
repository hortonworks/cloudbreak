package com.sequenceiq.cloudbreak.rotation.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StepProgressCleanupResponse {

    private List<StepProgressCleanupDescriptor> stepProgressCleanupDescriptors;

    public List<StepProgressCleanupDescriptor> getStepProgressCleanupDescriptors() {
        return stepProgressCleanupDescriptors;
    }

    public void setStepProgressCleanupDescriptors(List<StepProgressCleanupDescriptor> stepProgressCleanupDescriptors) {
        this.stepProgressCleanupDescriptors = stepProgressCleanupDescriptors;
    }

    public static StepProgressCleanupResponse of(List<StepProgressCleanupDescriptor> stepProgressCleanupDescriptors) {
        StepProgressCleanupResponse response = new StepProgressCleanupResponse();
        response.setStepProgressCleanupDescriptors(stepProgressCleanupDescriptors);
        return response;
    }
}
