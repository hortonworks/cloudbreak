package com.sequenceiq.cloudbreak.cloud.model;

public class CloudCredentialStatus {

    private final CloudCredential cloudCredential;

    private final CredentialStatus status;

    private final String statusReason;

    private final Exception exception;

    private final boolean defaultRegionChanged;

    public CloudCredentialStatus(CloudCredential cloudResource, CredentialStatus status) {
        this(cloudResource, status, null, null);
    }

    public CloudCredentialStatus(CloudCredential cloudCredential, CredentialStatus status, Exception exception, String statusReason) {
        this.cloudCredential = cloudCredential;
        this.status = status;
        this.statusReason = statusReason;
        this.exception = exception;
        this.defaultRegionChanged = false;
    }

    public CloudCredentialStatus(CloudCredentialStatus cloudCredentialStatus, boolean defaultRegionChanged) {
        this.cloudCredential = cloudCredentialStatus.getCloudCredential();
        this.status = cloudCredentialStatus.getStatus();
        this.statusReason = cloudCredentialStatus.getStatusReason();
        this.exception = cloudCredentialStatus.getException();
        this.defaultRegionChanged = defaultRegionChanged;
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

    public boolean isDefaultRegionChanged() {
        return defaultRegionChanged;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudCredentialStatus{"
                + "cloudCredential=" + cloudCredential
                + ", status=" + status
                + ", statusReason='" + statusReason + '\''
                + ", defaultRegionChanged='" + defaultRegionChanged + '\''
                + '}';
    }
    //END GENERATED CODE
}
