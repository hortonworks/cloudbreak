package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ProgressUpdateV4Event {

    private String stackName;

    private Long progress;

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public Long getProgress() {
        return progress;
    }

    public void setProgress(Long progress) {
        this.progress = progress;
    }

    public ProgressUpdateV4Event withStackName(String stackName) {
        this.stackName = stackName;
        return this;
    }

    public ProgressUpdateV4Event withProgress(long progress) {
        this.progress = progress;
        return this;
    }
}
