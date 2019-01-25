package com.sequenceiq.it.cloudbreak.filesystem;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;

public enum CloudStorageTypePathPrefix {

    S3("s3a") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters) {
            if (parameters == null || parameters instanceof S3CloudStorageV4Parameters) {
                request.setS3((S3CloudStorageV4Parameters) parameters);
            }
        }
    },
    WASB("wasb") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters) {
            if (parameters == null || parameters instanceof WasbCloudStorageV4Parameters) {
                request.setWasb((WasbCloudStorageV4Parameters) parameters);
            }
        }
    },
    ADLS("adl") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters) {
            if (parameters == null || parameters instanceof AdlsCloudStorageV4Parameters) {
                request.setAdls((AdlsCloudStorageV4Parameters) parameters);
            }
        }
    },
    GCS("gs") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters) {
            if (parameters == null || parameters instanceof GcsCloudStorageV4Parameters) {
                request.setGcs((GcsCloudStorageV4Parameters) parameters);
            }
        }
    },
    ADLS_GEN_2("adlsgen2") {
        @Override
        public void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters) {
            if (parameters == null || parameters instanceof AdlsGen2CloudStorageV4Parameters) {
                request.setAdlsGen2((AdlsGen2CloudStorageV4Parameters) parameters);
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

    public abstract void setParameterForRequest(CloudStorageV4Request request, CloudStorageV4Parameters parameters);

}
