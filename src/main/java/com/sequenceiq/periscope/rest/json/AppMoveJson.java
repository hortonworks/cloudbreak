package com.sequenceiq.periscope.rest.json;

public class AppMoveJson implements Json {

    private String appId;
    private String queue;
    private String message;

    public AppMoveJson() {
    }

    public AppMoveJson(String appId, String queue, String message) {
        this.appId = appId;
        this.queue = queue;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
