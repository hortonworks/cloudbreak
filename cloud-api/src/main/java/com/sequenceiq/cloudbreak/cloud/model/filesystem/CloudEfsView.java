package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.CloudIdentityType;

public class CloudEfsView extends CloudFileSystemView {
    private String instanceProfile;

    @JsonCreator
    public CloudEfsView(@JsonProperty("cloudIdentityType") CloudIdentityType cloudIdentityType) {
        super(cloudIdentityType);
    }

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
        if (!(o instanceof CloudEfsView)) {
            return false;
        }
        CloudEfsView that = (CloudEfsView) o;
        return StringUtils.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }
}
