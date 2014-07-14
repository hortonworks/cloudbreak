package com.sequenceiq.periscope.rest.json;

public class AppJson implements Json {

    private String appId;
    private String queue;

    public AppJson() {
    }

    public AppJson(String appId, String queue) {
        this.appId = appId;
        this.queue = queue;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}
