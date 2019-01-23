package com.sequenceiq.it.cloudbreak.filesystem;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;

public enum CloudStorageTypePathPrefix {

    S3("s3a") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters) {
            if (parameters == null || parameters instanceof S3CloudStorageParametersV4) {
                request.setS3((S3CloudStorageParametersV4) parameters);
            }
        }
    },
    WASB("wasb") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters) {
            if (parameters == null || parameters instanceof WasbCloudStorageParametersV4) {
                request.setWasb((WasbCloudStorageParametersV4) parameters);
            }
        }
    },
    ADLS("adl") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters) {
            if (parameters == null || parameters instanceof AdlsCloudStorageParametersV4) {
                request.setAdls((AdlsCloudStorageParametersV4) parameters);
            }
        }
    },
    GCS("gs") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters) {
            if (parameters == null || parameters instanceof GcsCloudStorageParametersV4) {
                request.setGcs((GcsCloudStorageParametersV4) parameters);
            }
        }
    },
    ADLS_GEN_2("adlsgen2") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters) {
            if (parameters == null || parameters instanceof AdlsGen2CloudStorageParametersV4) {
                request.setAdlsGen2((AdlsGen2CloudStorageParametersV4) parameters);
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

    public abstract void setParameterForRequest(CloudStorageV4Request request, CloudStorageParametersV4 parameters);

}
