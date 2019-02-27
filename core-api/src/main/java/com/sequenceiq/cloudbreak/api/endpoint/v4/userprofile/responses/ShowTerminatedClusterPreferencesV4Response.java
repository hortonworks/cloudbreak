package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ShowTerminatedClusterPreferencesV4Response {

    private String source;

    private Boolean active;

    private DurationV4Response timeout;

    public String getSource() {
        return source;
    }

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

    public void setSource(String source) {
        this.source = source;
    }
}
