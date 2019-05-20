package com.sequenceiq.it.cloudbreak.mock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CBVersion {

    private AppVersion app;

    public AppVersion getApp() {
        return app;
    }

    public void setApp(AppVersion app) {
        this.app = app;
    }
}
