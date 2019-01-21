package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DeleteInstanceByNameV4Filter {

    @QueryParam("forced")
    @DefaultValue("false")
    private Boolean forced;

    @QueryParam("instanceId")
    private String instanceId;

    public Boolean getForced() {
        return forced;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
