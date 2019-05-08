package com.sequenceiq.environment.api.environment.model.responses;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.environment.model.requests.EnvironmentNetworkV4Request;

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
