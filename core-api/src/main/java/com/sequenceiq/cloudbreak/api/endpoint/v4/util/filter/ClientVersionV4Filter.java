package com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ClientVersionV4Filter {

    @QueryParam("version")
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
