package com.sequenceiq.cloudbreak.audit.model;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

public class ConfigInfo {

    private final String requestId;

    private final String actorCrn;

    /**
     * Flag to enable/disable the export process
     */
    private final boolean enabled;

    /**
     * The credential name or CRN to use
     */
    private final String credentialName;

    /**
     * The region for the logs, e.g. S3 region
     */
    private final String storageRegion;

    /**
     * The destination for the logs, e.g. S3 bucket name
     */
    private final String storageLocation;

    public ConfigInfo(Builder builder) {
        this.requestId = builder.requestId;
        this.actorCrn = builder.actorCrn;
        this.enabled = builder.enabled;
        this.credentialName = builder.credentialName;
        this.storageRegion = builder.storageRegion;
        this.storageLocation = builder.storageLocation;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public String getStorageRegion() {
        return storageRegion;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ConfigInfo{" +
                "requestId='" + requestId + '\'' +
                ", actorCrn='" + actorCrn + '\'' +
                ", enabled=" + enabled +
                ", credentialName='" + credentialName + '\'' +
                ", storageRegion='" + storageRegion + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                '}';
    }

    public static class Builder {

        private String requestId;

        private String actorCrn;

        private boolean enabled;

        private String credentialName;

        private String storageRegion;

        private String storageLocation;

        public Builder withRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withActorCrn(String actorCrn) {
            this.actorCrn = actorCrn;
            return this;
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withCredentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        public Builder withStorageRegion(String storageRegion) {
            this.storageRegion = storageRegion;
            return this;
        }

        public Builder withStorageLocation(String storageLocation) {
            this.storageLocation = storageLocation;
            return this;
        }

        public ConfigInfo build() {
            checkArgument(Crn.isCrn(actorCrn), "Actor CRN must be valid.");
            checkArgument(StringUtils.isNotBlank(credentialName), "Credential name must be provided.");
            checkArgument(StringUtils.isNotBlank(storageLocation), "Storage location must be provided.");
            return new ConfigInfo(this);
        }
    }
}
