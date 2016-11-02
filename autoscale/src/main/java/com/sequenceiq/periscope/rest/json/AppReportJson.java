package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.api.model.Json;

public class AppReportJson implements Json {

    private String appId;

    private String user;

    private String queue;

    private String state;

    private String url;

    private long start;

    private long finish;

    private float progress;

    private int usedContainers;

    private int reservedContainers;

    private int usedMemory;

    private int usedVCores;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getFinish() {
        return finish;
    }

    public void setFinish(long finish) {
        this.finish = finish;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public int getUsedContainers() {
        return usedContainers;
    }

    public void setUsedContainers(int usedContainers) {
        this.usedContainers = usedContainers;
    }

    public int getReservedContainers() {
        return reservedContainers;
    }

    public void setReservedContainers(int reservedContainers) {
        this.reservedContainers = reservedContainers;
    }

    public int getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }

    public int getUsedVCores() {
        return usedVCores;
    }

    public void setUsedVCores(int usedVCores) {
        this.usedVCores = usedVCores;
    }
}
