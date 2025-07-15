package com.sequenceiq.remoteenvironment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DescribeEnvironmentPropertiesV2Response {

    @Schema(description = "Remote environment url", requiredMode = Schema.RequiredMode.REQUIRED)
    private String remoteEnvironmentUrl;

    public String getRemotenvironmentUrl() {
        return remoteEnvironmentUrl;
    }

    public void setRemoteEnvironmentUrl(String remoteEnvironmentUrl) {
        this.remoteEnvironmentUrl = remoteEnvironmentUrl;
    }

    @Override
    public String toString() {
        return "AdditionalRemoteEnvironmentPropertiesResponse{" +
                "remoteEnvironmentUrl='" + remoteEnvironmentUrl + '\'' +
                '}';
    }
}
