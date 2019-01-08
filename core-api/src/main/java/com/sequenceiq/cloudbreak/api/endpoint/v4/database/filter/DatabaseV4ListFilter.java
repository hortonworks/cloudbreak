package com.sequenceiq.cloudbreak.api.endpoint.v4.database.filter;

import javax.ws.rs.QueryParam;

public class DatabaseV4ListFilter {

    @QueryParam("environment")
    String environment;

    @QueryParam("attachGlobal")
    Boolean attachGlobal;

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
