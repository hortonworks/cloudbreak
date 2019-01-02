package com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.base;

import javax.ws.rs.QueryParam;

public class SmartSenseSubscriptionListV4Filter {

    @QueryParam("onlyDefault")
    private Boolean onlyDefault;

    public Boolean getOnlyDefault() {
        return onlyDefault;
    }

    public void setOnlyDefault(Boolean onlyDefault) {
        this.onlyDefault = onlyDefault;
    }
}
