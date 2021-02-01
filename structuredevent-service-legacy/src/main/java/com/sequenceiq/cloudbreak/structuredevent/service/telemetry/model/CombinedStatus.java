package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.model;

public class CombinedStatus {

    private String stackStatus;

    private String stackDetailedStatus;

    private String stackStatusReason;

    private String clusterStatus;

    private String clusterStatusReason;

    public String getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(String stackStatus) {
        this.stackStatus = stackStatus;
    }

    public String getStackDetailedStatus() {
        return stackDetailedStatus;
    }

    public void setStackDetailedStatus(String stackDetailedStatus) {
        this.stackDetailedStatus = stackDetailedStatus;
    }

    public String getStackStatusReason() {
        return stackStatusReason;
    }

    public void setStackStatusReason(String stackStatusReason) {
        this.stackStatusReason = stackStatusReason;
    }

    public String getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(String clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public String getClusterStatusReason() {
        return clusterStatusReason;
    }

    public void setClusterStatusReason(String clusterStatusReason) {
        this.clusterStatusReason = clusterStatusReason;
    }
}
