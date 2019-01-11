package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.BaseFileSystem;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3FileSystem extends BaseFileSystem {

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
        if (!(o instanceof S3FileSystem)) {
            return false;
        }
        S3FileSystem that = (S3FileSystem) o;
        return Objects.equals(getInstanceProfile(), that.getInstanceProfile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceProfile());
    }

}
