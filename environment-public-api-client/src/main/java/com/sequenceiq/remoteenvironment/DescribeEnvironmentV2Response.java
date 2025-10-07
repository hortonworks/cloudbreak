package com.sequenceiq.remoteenvironment;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeEnvironmentV2Response extends DescribeEnvironmentResponse {

    @Schema(description = "Additional remote env properties")
    private DescribeEnvironmentPropertiesV2Response additionalProperties;

    public DescribeEnvironmentPropertiesV2Response getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(DescribeEnvironmentPropertiesV2Response additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public DescribeEnvironmentResponse toV1Response() {
        DescribeEnvironmentResponse describeEnvironmentResponse = new DescribeEnvironmentResponse();
        describeEnvironmentResponse.setEnvironment(getEnvironment());
        return describeEnvironmentResponse;
    }

    @Override
    public String toString() {
        return "DescribeRemoteEnvironmentResponse{" +
                "additionalProperties='" + additionalProperties + '\'' +
                '}';
    }
}
