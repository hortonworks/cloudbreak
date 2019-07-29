package com.sequenceiq.cloudbreak.domain.cloudstorage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

public class CloudIdentity {

    private CloudIdentityType identityType;

    private WasbIdentity wasbIdentity;

    private S3Identity s3Identity;

    @JsonIgnore
    public FileSystemType getFileSystemType() {
        if (wasbIdentity != null) {
            return wasbIdentity.getType();
        }
        if (s3Identity != null) {
            return s3Identity.getType();
        }
        throw new IllegalStateException("No identity is present! WASB or S3 identity should be stored.");
    }

    public CloudIdentityType getIdentityType() {
        return identityType;
    }

    public void setIdentityType(CloudIdentityType identityType) {
        this.identityType = identityType;
    }

    public WasbIdentity getWasbIdentity() {
        return wasbIdentity;
    }

    public void setWasbIdentity(WasbIdentity wasbIdentity) {
        this.wasbIdentity = wasbIdentity;
    }

    public S3Identity getS3Identity() {
        return s3Identity;
    }

    public void setS3Identity(S3Identity s3Identity) {
        this.s3Identity = s3Identity;
    }
}
