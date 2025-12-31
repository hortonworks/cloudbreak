package com.sequenceiq.environment.api.v1.platformresource.model.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataLakeSupportRequirements extends BaseSupportRequirements {

    @Override
    public String toString() {
        return "DataLakeSupportRequirements{" + super.toString() + "}";
    }
}
