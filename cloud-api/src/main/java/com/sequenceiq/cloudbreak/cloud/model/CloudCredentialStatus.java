package com.sequenceiq.cloudbreak.cloud.model;

public class CloudCredentialStatus {

    private CloudCredential cloudCredential;

    private CredentialStatus status;

    private String statusReason;

    private Exception exception;

    public CloudCredentialStatus(CloudCredential cloudResource, CredentialStatus status) {
        this(cloudResource, status, null, null);
    }

    public CloudCredentialStatus(CloudCredential cloudCredential, CredentialStatus status, Exception exception, String statusReason) {
        this.cloudCredential = cloudCredential;
        this.status = status;
        this.statusReason = statusReason;
        this.exception = exception;
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

    public Exception getException() {
        return exception;
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
