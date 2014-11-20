package com.sequenceiq.cloudbreak.controller.json;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.conf.EnvironmentValidator;

public class SubscribeRequest {

    @NotNull
    @Pattern(regexp = EnvironmentValidator.URL_PATTERN,
            message = "Must be a proper URL!")
    private String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
