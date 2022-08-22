package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.CloudIdentityType;

public class CloudS3View extends CloudFileSystemView {

    private String instanceProfile;

    private String s3GuardDynamoTableName;

    @JsonCreator
    public CloudS3View(@JsonProperty("cloudIdentityType") CloudIdentityType cloudIdentityType) {
        super(cloudIdentityType);
    }

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    public String getS3GuardDynamoTableName() {
        return s3GuardDynamoTableName;
    }

    public void setS3GuardDynamoTableName(String s3GuardDynamoTableName) {
        this.s3GuardDynamoTableName = s3GuardDynamoTableName;
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
        return Objects.equals(instanceProfile, that.instanceProfile)
                && Objects.equals(s3GuardDynamoTableName, that.s3GuardDynamoTableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile, s3GuardDynamoTableName);
    }
}
