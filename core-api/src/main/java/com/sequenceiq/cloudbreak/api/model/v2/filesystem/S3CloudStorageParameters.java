package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidS3CloudStorageParameters;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
}
