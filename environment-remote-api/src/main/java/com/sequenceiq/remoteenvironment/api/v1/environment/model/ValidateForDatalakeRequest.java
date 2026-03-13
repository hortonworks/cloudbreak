package com.sequenceiq.remoteenvironment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateForDatalakeRequest extends DescribeRemoteEnvironment {

    @Override
    public String toString() {
        return "ValidateForDatalakeRequest{" + super.toString() + '}';
    }
}
