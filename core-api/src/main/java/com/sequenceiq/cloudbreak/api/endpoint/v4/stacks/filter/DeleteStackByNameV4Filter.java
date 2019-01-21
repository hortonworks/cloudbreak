package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DeleteStackByNameV4Filter {

    @QueryParam("forced")
    @DefaultValue("false")
    private Boolean forced;

    @QueryParam("deleteDependencies")
    @DefaultValue("false")
    private Boolean deleteDependencies;

    public Boolean getForced() {
        return forced;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    public Boolean getDeleteDependencies() {
        return deleteDependencies;
    }

    public void setDeleteDependencies(Boolean deleteDependencies) {
        this.deleteDependencies = deleteDependencies;
    }
}
