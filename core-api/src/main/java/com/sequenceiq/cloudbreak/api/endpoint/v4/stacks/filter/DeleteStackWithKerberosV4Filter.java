package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DeleteStackWithKerberosV4Filter {

    @QueryParam("withStackDelete")
    @DefaultValue("false")
    private Boolean withStackDelete;

    @QueryParam("deleteDependencies")
    @DefaultValue("false")
    private Boolean deleteDependencies;

    public Boolean getWithStackDelete() {
        return withStackDelete;
    }

    public void setWithStackDelete(Boolean withStackDelete) {
        this.withStackDelete = withStackDelete;
    }

    public Boolean getDeleteDependencies() {
        return deleteDependencies;
    }

    public void setDeleteDependencies(Boolean deleteDependencies) {
        this.deleteDependencies = deleteDependencies;
    }
}
