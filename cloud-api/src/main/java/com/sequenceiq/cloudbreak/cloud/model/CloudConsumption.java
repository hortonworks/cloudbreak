package com.sequenceiq.cloudbreak.cloud.model;

public class CloudConsumption {

    private final CloudCredential cloudCredential;

    private final String storageLocation;

    private final String region;

    private final String environmentCrn;

    private CloudConsumption(Builder builder) {
        cloudCredential = builder.cloudCredential;
        storageLocation = builder.storageLocation;
        region = builder.region;
        environmentCrn = builder.environmentCrn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public String getRegion() {
        return region;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    @Override
    public String toString() {
        return "CloudConsumption{" +
                "CloudCredential='" + cloudCredential + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                ", region='" + region + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }

    public static final class Builder {

            private CloudCredential cloudCredential;

            private String storageLocation;

            private String region;

            private String environmentCrn;

            private Builder() {
            }

            public Builder withCloudCredential(CloudCredential cloudCredential) {
                this.cloudCredential = cloudCredential;
                return this;
            }

            public Builder withStorageLocation(String storageLocation) {
                this.storageLocation = storageLocation;
                return this;
            }

            public Builder withRegion(String region) {
                this.region = region;
                return this;
            }

            public Builder withEnvironmentCrn(String environmentCrn) {
                this.environmentCrn = environmentCrn;
                return this;
            }

            public CloudConsumption build() {
                return new CloudConsumption(this);
            }

        }
    }

