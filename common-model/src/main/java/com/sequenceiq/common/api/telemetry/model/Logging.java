package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Logging implements Serializable {

    private String storageLocation;

    private S3CloudStorageV1Parameters s3;

    private WasbCloudStorageV1Parameters wasb;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public WasbCloudStorageV1Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV1Parameters wasb) {
        this.wasb = wasb;
    }
}
