package com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class BlueprintNameV4Filter {

    @QueryParam("blueprintName")
    private String blueprintName;

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }
}
