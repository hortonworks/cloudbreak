package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "DetailedEnvironmentV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private CompactRegionResponse regions;

        private boolean freeIpaCreation;

        private String cloudPlatform;

        private CredentialResponse credential;

        private LocationResponse location;

        private EnvironmentNetworkResponse network;

        private EnvironmentStatus environmentStatus;

        private Builder() {
        }

        public static Builder aDetailedEnvironmentResponse() {
            return new Builder();
        }

        public Builder withCrn(String id) {
            this.crn = id;
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

        public Builder withCredential(CredentialResponse credential) {
            this.credential = credential;
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

        public Builder withFreeIpaCreation(boolean freeIpaCreation) {
            this.freeIpaCreation = freeIpaCreation;
            return this;
        }

        public Builder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public DetailedEnvironmentResponse build() {
            DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
            detailedEnvironmentResponse.setCrn(crn);
            detailedEnvironmentResponse.setName(name);
            detailedEnvironmentResponse.setDescription(description);
            detailedEnvironmentResponse.setRegions(regions);
            detailedEnvironmentResponse.setCloudPlatform(cloudPlatform);
            detailedEnvironmentResponse.setCredential(credential);
            detailedEnvironmentResponse.setLocation(location);
            detailedEnvironmentResponse.setNetwork(network);
            detailedEnvironmentResponse.setCreateFreeIpa(freeIpaCreation);
            detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            return detailedEnvironmentResponse;
        }
    }
}
