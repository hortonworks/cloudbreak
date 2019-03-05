package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockCredentialV4Parameters implements CredentialV4Parameters {

    @ApiModelProperty(hidden = true)
    private String mockEndpoint;

    public String getMockEndpoint() {
        return mockEndpoint;
    }

    public void setMockEndpoint(String mockEndpoint) {
        this.mockEndpoint = mockEndpoint;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mockEndpoint", mockEndpoint);
        return map;
    }

}
