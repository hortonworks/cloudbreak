package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.services.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidS3CloudStorageParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@ValidS3CloudStorageParameters
public class S3CloudStorageV4Parameters implements CloudStorageV4Parameters {

    @ApiModelProperty
    @NotNull
    private String instanceProfile;

    @ApiModelProperty(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.S3;
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
        if (!(o instanceof S3CloudStorageV4Parameters)) {
            return false;
        }
        S3CloudStorageV4Parameters that = (S3CloudStorageV4Parameters) o;
        return Objects.equals(instanceProfile, that.instanceProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceProfile);
    }

}
