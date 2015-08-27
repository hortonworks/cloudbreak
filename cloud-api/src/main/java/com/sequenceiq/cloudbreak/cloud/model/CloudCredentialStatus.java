package com.sequenceiq.cloudbreak.cloud.model;

public class CloudCredentialStatus {

    private CloudCredential cloudCredential;

    private CredentialStatus status;

    private String statusReason;

    public CloudCredentialStatus(CloudCredential cloudResource, CredentialStatus status) {
        this(cloudResource, status, null);
    }

    public CloudCredentialStatus(CloudCredential cloudCredential, CredentialStatus status, String statusReason) {
        this.cloudCredential = cloudCredential;
        this.status = status;
        this.statusReason = statusReason;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    //BEGIN GENERATED CODE

    @Override
    public String toString() {
        return "CloudCredentialStatus{" +
                "cloudCredential=" + cloudCredential +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }


    //END GENERATED CODE


}
