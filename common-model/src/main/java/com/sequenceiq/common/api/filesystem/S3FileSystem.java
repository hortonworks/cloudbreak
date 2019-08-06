package com.sequenceiq.common.api.filesystem;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3FileSystem extends BaseFileSystem {

    private String instanceProfile;

    private String s3GuardDynamoTableName;

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
        if (!(o instanceof S3FileSystem)) {
            return false;
        }
        S3FileSystem that = (S3FileSystem) o;
        return Objects.equals(instanceProfile, that.instanceProfile)
                && Objects.equals(s3GuardDynamoTableName, that.s3GuardDynamoTableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile, s3GuardDynamoTableName);
    }
}
