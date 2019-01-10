package com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter;

import javax.ws.rs.QueryParam;

public class StackVersionV4Filter {

    @QueryParam("stackVersion")
    private String stackVersion;

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }
}
