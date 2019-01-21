package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class GetAllStackV4Filter {

    @QueryParam("environment")
    private String environment;

    @QueryParam("onlyDatalakes")
    private boolean onlyDatalakes;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isOnlyDatalakes() {
        return onlyDatalakes;
    }

    public void setOnlyDatalakes(boolean onlyDatalakes) {
        this.onlyDatalakes = onlyDatalakes;
    }
}
