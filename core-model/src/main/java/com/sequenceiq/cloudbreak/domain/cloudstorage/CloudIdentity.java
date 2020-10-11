package com.sequenceiq.cloudbreak.domain.cloudstorage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

public class CloudIdentity {

    private CloudIdentityType identityType;

    private WasbIdentity wasbIdentity;

    private S3Identity s3Identity;

    private AdlsGen2Identity adlsGen2Identity;

    private GcsIdentity gcsIdentity;

    @JsonIgnore
    public FileSystemType getFileSystemType() {
        if (wasbIdentity != null) {
            return wasbIdentity.getType();
        } else if (s3Identity != null) {
            return s3Identity.getType();
        } else if (adlsGen2Identity != null) {
            return adlsGen2Identity.getType();
        } else if (gcsIdentity != null) {
            return gcsIdentity.getType();
        }
        throw new IllegalStateException("No identity is present! WASB, ABFS, GCS or S3 identity should be stored.");
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

    public AdlsGen2Identity getAdlsGen2Identity() {
        return adlsGen2Identity;
    }

    public void setAdlsGen2Identity(AdlsGen2Identity adlsGen2Identity) {
        this.adlsGen2Identity = adlsGen2Identity;
    }

    public GcsIdentity getGcsIdentity() {
        return gcsIdentity;
    }

    public void setGcsIdentity(GcsIdentity gcsIdentity) {
        this.gcsIdentity = gcsIdentity;
    }
}
