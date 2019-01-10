package com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SecurityRulesV4Filter {

    @QueryParam("knoxEnabled")
    @DefaultValue("false")
    private boolean knoxEnabled;

    public boolean isKnoxEnabled() {
        return knoxEnabled;
    }

    public void setKnoxEnabled(boolean knoxEnabled) {
        this.knoxEnabled = knoxEnabled;
    }
}
