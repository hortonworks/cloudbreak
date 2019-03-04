package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

public class CloudS3View extends CloudFileSystemView {

    private String instanceProfile;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CloudS3View)) {
            return false;
        }
        CloudS3View that = (CloudS3View) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

}
