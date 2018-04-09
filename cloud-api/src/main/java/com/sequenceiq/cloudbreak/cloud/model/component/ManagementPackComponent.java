package com.sequenceiq.cloudbreak.cloud.model.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagementPackComponent implements Serializable {
    private String mpackUrl;

    private boolean purge;

    private List<String> purgeList = new ArrayList<>();

    private boolean force;

    private boolean stackDefault;

    private boolean preInstalled;

    public String getMpackUrl() {
        return mpackUrl;
    }

    public void setMpackUrl(String mpackUrl) {
        this.mpackUrl = mpackUrl;
    }

    public boolean isPurge() {
        return purge;
    }

    public void setPurge(boolean purge) {
        this.purge = purge;
    }

    public List<String> getPurgeList() {
        return purgeList;
    }

    public void setPurgeList(List<String> purgeList) {
        this.purgeList = purgeList;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isStackDefault() {
        return stackDefault;
    }

    public void setStackDefault(boolean stackDefault) {
        this.stackDefault = stackDefault;
    }

    public boolean isPreInstalled() {
        return preInstalled;
    }

    public void setPreInstalled(boolean preInstalled) {
        this.preInstalled = preInstalled;
    }
}
