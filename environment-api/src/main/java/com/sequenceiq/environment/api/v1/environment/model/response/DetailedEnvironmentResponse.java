package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "DetailedEnvironmentV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private boolean createFreeIpa;

        private CompactRegionResponse regions;

        private String cloudPlatform;

        private CredentialResponse credential;

        private LocationResponse location;

        private EnvironmentNetworkResponse network;

        private TelemetryResponse telemetry;

        private EnvironmentStatus environmentStatus;

        private String statusReason;

        private Long created;

        private EnvironmentAuthenticationResponse authentication;

        private SecurityAccessResponse securityAccess;

        private CloudStorageResponse logCloudStorage;

        private Tunnel tunnel;

        private IdBrokerMappingSource idBrokerMappingSource;

        private Builder() {
        }

        public static Builder builder() {
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

        public Builder withTelemetry(TelemetryResponse telemetry) {
            this.telemetry = telemetry;
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

        public Builder withAuthentication(EnvironmentAuthenticationResponse authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder withSecurityAccess(SecurityAccessResponse securityAccess) {
            this.securityAccess = securityAccess;
            return this;
        }

        public Builder withLogCloudStorage(CloudStorageResponse logCloudStorage) {
            this.logCloudStorage = logCloudStorage;
            return this;
        }

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
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
            detailedEnvironmentResponse.setCreateFreeIpa(createFreeIpa);
            detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            detailedEnvironmentResponse.setStatusReason(statusReason);
            detailedEnvironmentResponse.setCreated(created);
            detailedEnvironmentResponse.setAuthentication(authentication);
            detailedEnvironmentResponse.setTelemetry(telemetry);
            detailedEnvironmentResponse.setSecurityAccess(securityAccess);
            detailedEnvironmentResponse.setTunnel(tunnel);
            detailedEnvironmentResponse.setLogCloudStorage(logCloudStorage);
            detailedEnvironmentResponse.setIdBrokerMappingSource(idBrokerMappingSource);
            return detailedEnvironmentResponse;
        }
    }
}
