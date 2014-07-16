package com.sequenceiq.periscope.rest.json;

public class AppJson {

    private String appId;
    private int priority;

    public AppJson() {
    }

    public AppJson(String appId, int priority) {
        this.appId = appId;
        this.priority = priority;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
