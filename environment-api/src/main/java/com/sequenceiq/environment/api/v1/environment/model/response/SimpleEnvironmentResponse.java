package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "SimpleEnvironmentV1Response")
public class SimpleEnvironmentResponse extends EnvironmentBaseResponse {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private boolean createFreeIpa;

        private CompactRegionResponse regions;

        private String cloudPlatform;

        private CredentialResponse credentialResponse;

        private LocationResponse location;

        private EnvironmentNetworkResponse network;

        private EnvironmentStatus environmentStatus;

        private String statusReason;

        private Long created;

        private Builder() {
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withRegions(CompactRegionResponse regions) {
            this.regions = regions;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withCredential(CredentialResponse credentialResponse) {
            this.credentialResponse = credentialResponse;
            return this;
        }

        public Builder withLocation(LocationResponse location) {
            this.location = location;
            return this;
        }

        public Builder withNetwork(EnvironmentNetworkResponse network) {
            this.network = network;
            return this;
        }

        public Builder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public Builder withCreateFreeIpa(boolean createFreeIpa) {
            this.createFreeIpa = createFreeIpa;
            return this;
        }

        public Builder withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public SimpleEnvironmentResponse build() {
            SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();
            simpleEnvironmentResponse.setCrn(crn);
            simpleEnvironmentResponse.setName(name);
            simpleEnvironmentResponse.setDescription(description);
            simpleEnvironmentResponse.setRegions(regions);
            simpleEnvironmentResponse.setCloudPlatform(cloudPlatform);
            simpleEnvironmentResponse.setCredential(credentialResponse);
            simpleEnvironmentResponse.setLocation(location);
            simpleEnvironmentResponse.setNetwork(network);
            simpleEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            simpleEnvironmentResponse.setCreateFreeIpa(createFreeIpa);
            simpleEnvironmentResponse.setStatusReason(statusReason);
            simpleEnvironmentResponse.setCreated(created);
            return simpleEnvironmentResponse;
        }
    }
}
