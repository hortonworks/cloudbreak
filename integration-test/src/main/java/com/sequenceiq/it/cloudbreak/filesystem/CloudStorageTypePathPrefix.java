package com.sequenceiq.it.cloudbreak.filesystem;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;

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
    ABFS("abfs") {
        @Override
        public void setParameterForRequest(CloudStorageRequest request, CloudStorageParameters parameters) {
            if (parameters == null || parameters instanceof AbfsCloudStorageParameters) {
                request.setAbfs((AbfsCloudStorageParameters) parameters);
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
