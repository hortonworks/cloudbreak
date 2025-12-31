package com.sequenceiq.environment.api.v1.platformresource.model.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataHubSupportRequirements extends BaseSupportRequirements {

    @Override
    public String toString() {
        return "DataHubSupportRequirements{" + super.toString() + "}";
    }
}
