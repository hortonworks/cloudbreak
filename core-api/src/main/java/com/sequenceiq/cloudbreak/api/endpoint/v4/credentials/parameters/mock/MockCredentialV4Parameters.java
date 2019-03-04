package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class MockCredentialV4Parameters extends MappableBase {

    @ApiModelProperty(hidden = true)
    private String mockEndpoint;

    public String getMockEndpoint() {
        return mockEndpoint;
    }

    public void setMockEndpoint(String mockEndpoint) {
        this.mockEndpoint = mockEndpoint;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("mockEndpoint", mockEndpoint);
        return map;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        mockEndpoint = getParameterOrNull(parameters, "mockEndpoint");
    }

}
