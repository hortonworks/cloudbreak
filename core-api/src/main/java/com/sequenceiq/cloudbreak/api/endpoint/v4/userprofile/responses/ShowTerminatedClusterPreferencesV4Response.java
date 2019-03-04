package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ShowTerminatedClusterPreferencesV4Response {

    private Boolean active;

    private DurationV4Response timeout;

    public Boolean getActive() {
        return active;
    }

    public DurationV4Response getTimeout() {
        return timeout;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setTimeout(DurationV4Response timeout) {
        this.timeout = timeout;
    }
}
