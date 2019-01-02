package com.sequenceiq.cloudbreak.api.endpoint.v4.common.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ListV4Filter {

    @QueryParam("environment")
    private String environment;

    @QueryParam("attachGlobal")
    private Boolean attachGlobal;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Boolean getAttachGlobal() {
        return attachGlobal;
    }

    public void setAttachGlobal(Boolean attachGlobal) {
        this.attachGlobal = attachGlobal;
    }
}
