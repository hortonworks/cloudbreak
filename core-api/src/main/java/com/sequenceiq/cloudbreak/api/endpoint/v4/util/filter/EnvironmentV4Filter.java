package com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter;

import javax.ws.rs.QueryParam;

public class EnvironmentV4Filter {

    @QueryParam("environment")
    private String environment;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
