package com.sequenceiq.it.cloudbreak.filesystem;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;

public enum CloudStorageTypePathPrefix {

    S3("s3a") {
        @Override
        public void setParameterForRequest(@Nonnull CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof S3CloudStorageParameters) {
                request.setS3((S3CloudStorageParameters) parameters);
            }
        }
    },
    WASB("wasb") {
        @Override
        public void setParameterForRequest(@Nonnull CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof WasbCloudStorageParameters) {
                request.setWasb((WasbCloudStorageParameters) parameters);
            }
        }
    },
    ADLS("adl") {
        @Override
        public void setParameterForRequest(@Nonnull CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof AdlsCloudStorageParameters) {
                request.setAdls((AdlsCloudStorageParameters) parameters);
            }
        }
    },
    GCS("gs") {
        @Override
        public void setParameterForRequest(CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof GcsCloudStorageParameters) {
                request.setGcs((GcsCloudStorageParameters) parameters);
            }
        }
    },
    ADLS_GEN_2("adlsgen2") {
        @Override
        public void setParameterForRequest(CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof AdlsGen2CloudStorageParameters) {
                request.setAdlsGen2((AdlsGen2CloudStorageParameters) parameters);
            }
        }
    };

    private final String prefix;

    CloudStorageTypePathPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public abstract void setParameterForRequest(CloudStorageRequest request, CloudStorageParameters parameters);

}
