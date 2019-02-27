package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel
@NotNull
public class ShowTerminatedClustersPreferencesV4Request implements JsonEntity {

    @NotNull
    private Boolean active;

    @Valid
    private DurationV4Request timeout;

    public Boolean isActive() {
        return active;
    }

    public DurationV4Request getTimeout() {
        return timeout;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setTimeoutSeconds(DurationV4Request timeout) {
        this.timeout = timeout;
    }
}
