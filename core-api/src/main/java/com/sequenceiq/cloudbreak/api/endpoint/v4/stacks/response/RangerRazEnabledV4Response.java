package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class RangerRazEnabledV4Response {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRazEnabled;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRMsEnabled;

    public RangerRazEnabledV4Response() {
    }

    public RangerRazEnabledV4Response(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean isRangerRMsEnabled() {
        return rangerRMsEnabled;
    }

    public void setRangerRMsEnabled(boolean rangerRMsEnabled) {
        this.rangerRMsEnabled = rangerRMsEnabled;
    }

    @Override
    public String toString() {
        return "RangerRazEnabledV4Response{" +
                "rangerRazEnabled=" + rangerRazEnabled +
                ", rangerRmsEnabled=" + rangerRMsEnabled +
                '}';
    }
}
