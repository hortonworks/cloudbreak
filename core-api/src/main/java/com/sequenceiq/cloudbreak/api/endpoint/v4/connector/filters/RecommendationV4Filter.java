package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.filters;

import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationV4Filter extends PlatformResourceV4Filter {

    @QueryParam("blueprintName")
    private String blueprintName;

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }
}
