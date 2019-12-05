package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;

import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "SimpleEnvironmentV1Response")
public class SimpleEnvironmentResponse extends EnvironmentBaseResponse {

    private CredentialViewResponse credential;

    public CredentialViewResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialViewResponse credential) {
        this.credential = credential;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private String creator;

        private boolean createFreeIpa;

        private CompactRegionResponse regions;

        private String cloudPlatform;

        private CredentialViewResponse credentialViewResponse;

        private LocationResponse location;

        private TelemetryResponse telemetry;

        private EnvironmentNetworkResponse network;

        private EnvironmentStatus environmentStatus;

        private String statusReason;

        private Long created;

        private Tunnel tunnel;

        private String adminGroupName;

        private AwsEnvironmentParameters aws;

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

        public Builder withCredentialView(CredentialViewResponse credentialResponse) {
            this.credentialViewResponse = credentialResponse;
            return this;
        }

        public Builder withCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder withTelemetry(TelemetryResponse telemetry) {
            this.telemetry = telemetry;
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

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return this;
        }

        public Builder withAws(AwsEnvironmentParameters aws) {
            this.aws = aws;
            return this;
        }

        public SimpleEnvironmentResponse build() {
            SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();
            simpleEnvironmentResponse.setCrn(crn);
            simpleEnvironmentResponse.setName(name);
            simpleEnvironmentResponse.setCreator(creator);
            simpleEnvironmentResponse.setDescription(description);
            simpleEnvironmentResponse.setRegions(regions);
            simpleEnvironmentResponse.setCloudPlatform(cloudPlatform);
            simpleEnvironmentResponse.setCredential(credentialViewResponse);
            simpleEnvironmentResponse.setLocation(location);
            simpleEnvironmentResponse.setNetwork(network);
            simpleEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            simpleEnvironmentResponse.setCreateFreeIpa(createFreeIpa);
            simpleEnvironmentResponse.setStatusReason(statusReason);
            simpleEnvironmentResponse.setCreated(created);
            simpleEnvironmentResponse.setTelemetry(telemetry);
            simpleEnvironmentResponse.setTunnel(tunnel);
            simpleEnvironmentResponse.setAws(aws);
            simpleEnvironmentResponse.setAdminGroupName(adminGroupName);
            return simpleEnvironmentResponse;
        }
    }
}
