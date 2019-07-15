package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.common.api.cloudstorage.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.FileSystemType;

public class CloudStorageDto {

    private AdlsCloudStorageV1Parameters adls;

    private WasbCloudStorageV1Parameters wasb;

    private GcsCloudStorageV1Parameters gcs;

    private S3CloudStorageV1Parameters s3;

    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    private FileSystemType fileSystemType;

    private String baseLocation;

    public static Builder builder() {
        return new Builder();
    }

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    public AdlsCloudStorageV1Parameters getAdls() {
        return adls;
    }

    public void setAdls(AdlsCloudStorageV1Parameters adls) {
        this.adls = adls;
    }

    public WasbCloudStorageV1Parameters getWasb() {
        return wasb;
    }

    public void setWasb(WasbCloudStorageV1Parameters wasb) {
        this.wasb = wasb;
    }

    public GcsCloudStorageV1Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV1Parameters gcs) {
        this.gcs = gcs;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }


    public static final class Builder {
        private AdlsCloudStorageV1Parameters adls;

        private WasbCloudStorageV1Parameters wasb;

        private GcsCloudStorageV1Parameters gcs;

        private S3CloudStorageV1Parameters s3;

        private AdlsGen2CloudStorageV1Parameters adlsGen2;

        private FileSystemType fileSystemType;

        private String baseLocation;

        private Builder() {
        }

        public Builder withAdls(AdlsCloudStorageV1Parameters adls) {
            this.adls = adls;
            return this;
        }

        public Builder withWasb(WasbCloudStorageV1Parameters wasb) {
            this.wasb = wasb;
            return this;
        }

        public Builder withGcs(GcsCloudStorageV1Parameters gcs) {
            this.gcs = gcs;
            return this;
        }

        public Builder withS3(S3CloudStorageV1Parameters s3) {
            this.s3 = s3;
            return this;
        }

        public Builder withAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
            this.adlsGen2 = adlsGen2;
            return this;
        }

        public Builder withFileSystemType(FileSystemType fileSystemType) {
            this.fileSystemType = fileSystemType;
            return this;
        }

        public Builder withBaseLocation(String baseLocation) {
            this.baseLocation = baseLocation;
            return this;
        }

        public CloudStorageDto build() {
            CloudStorageDto cloudStorageDto = new CloudStorageDto();
            cloudStorageDto.setAdls(adls);
            cloudStorageDto.setWasb(wasb);
            cloudStorageDto.setGcs(gcs);
            cloudStorageDto.setS3(s3);
            cloudStorageDto.setAdlsGen2(adlsGen2);
            cloudStorageDto.setFileSystemType(fileSystemType);
            cloudStorageDto.setBaseLocation(baseLocation);
            return cloudStorageDto;
        }
    }
}
