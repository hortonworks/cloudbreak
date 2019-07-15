package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.common.api.cloudstorage.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageBase;

import io.swagger.annotations.ApiModel;

@ApiModel("CloudStorageV1Response")
public class CloudStorageResponse extends CloudStorageBase {

    public static Builder builder() {
        return new Builder();
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

        public CloudStorageResponse build() {
            CloudStorageResponse cloudStorageResponse = new CloudStorageResponse();
            cloudStorageResponse.setAdls(adls);
            cloudStorageResponse.setWasb(wasb);
            cloudStorageResponse.setGcs(gcs);
            cloudStorageResponse.setS3(s3);
            cloudStorageResponse.setAdlsGen2(adlsGen2);
            cloudStorageResponse.setFileSystemType(fileSystemType);
            cloudStorageResponse.setBaseLocation(baseLocation);
            return cloudStorageResponse;
        }
    }
}
