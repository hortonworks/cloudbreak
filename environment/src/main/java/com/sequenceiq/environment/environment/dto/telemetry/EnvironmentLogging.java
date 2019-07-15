package com.sequenceiq.environment.environment.dto.telemetry;

public class EnvironmentLogging extends CommonTelemetryParams {

    private String storageLocation;

    private S3CloudStorageParameters s3;

    private WasbCloudStorageParameters wasb;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public S3CloudStorageParameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageParameters s3) {
        this.s3 = s3;
    }

    public WasbCloudStorageParameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageParameters wasb) {
        this.wasb = wasb;
    }
}
