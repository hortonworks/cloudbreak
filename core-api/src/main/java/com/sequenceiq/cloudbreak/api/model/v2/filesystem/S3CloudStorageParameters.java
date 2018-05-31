package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidS3CloudStorageParameters;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel
@ValidS3CloudStorageParameters
public class S3CloudStorageParameters implements CloudStorageParameters {

    @ApiModelProperty
    private String instanceProfile;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    @ApiModelProperty(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.S3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof S3CloudStorageParameters)) {
            return false;
        }
        S3CloudStorageParameters that = (S3CloudStorageParameters) o;
        return Objects.equals(getInstanceProfile(), that.getInstanceProfile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceProfile());
    }

}
