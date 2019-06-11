package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentNetworkV4Response extends EnvironmentNetworkV4Request {
    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
