package com.sequenceiq.cloudbreak.cloud.model.filesystem;

public class CloudGcsView extends CloudFileSystemView {

    private String serviceAccountEmail;

    public CloudGcsView() {
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }
}
