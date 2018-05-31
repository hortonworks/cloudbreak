package com.sequenceiq.cloudbreak.api.model.v2.filesystem;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.validation.ValidGcsCloudStorageParameters;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

@ApiModel
@ValidGcsCloudStorageParameters
public class GcsCloudStorageParameters implements CloudStorageParameters {

    @ApiModelProperty
    private String serviceAccountEmail;

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @ApiModelProperty(hidden = true)
    @Override
    public FileSystemType getType() {
        return FileSystemType.GCS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GcsCloudStorageParameters)) {
            return false;
        }
        GcsCloudStorageParameters that = (GcsCloudStorageParameters) o;
        return Objects.equals(getServiceAccountEmail(), that.getServiceAccountEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServiceAccountEmail());
    }

}
