package com.sequenceiq.cloudbreak.cloud.model.filesystem;

public class CloudS3View extends CloudFileSystemView {

    private String instanceProfile;

    public CloudS3View() {
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }
}
