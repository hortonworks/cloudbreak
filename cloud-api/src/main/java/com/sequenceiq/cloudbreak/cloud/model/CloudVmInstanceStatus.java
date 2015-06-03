package com.sequenceiq.cloudbreak.cloud.model;

public class CloudVmInstanceStatus {

    private Instance instance;

    private InstanceStatus status;

    private String statusReason;

    public CloudVmInstanceStatus(Instance instance, InstanceStatus status, String statusReason) {
        this.instance = instance;
        this.status = status;
        this.statusReason = statusReason;
    }

    public CloudVmInstanceStatus(Instance instance, InstanceStatus status) {
        this.instance = instance;
        this.status = status;
    }

    public Instance getInstance() {
        return instance;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudVmInstanceStatus{" +
                "instance=" + instance +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
    //BEGIN GENERATED CODE
}
