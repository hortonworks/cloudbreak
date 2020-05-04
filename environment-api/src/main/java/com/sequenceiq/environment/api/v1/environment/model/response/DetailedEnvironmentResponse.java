package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "DetailedEnvironmentV1Response")
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_RESPONSE)
    private CredentialResponse credential;

    @ApiModelProperty(EnvironmentModelDescription.PROXYCONFIG_RESPONSE)
    private ProxyResponse proxyConfig;

    public CredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialResponse credential) {
        this.credential = credential;
    }

    public ProxyResponse getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyResponse proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private String creator;

        private boolean createFreeIpa;

        private FreeIpaResponse freeIpa;

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

        private Tunnel tunnel;

        private String adminGroupName;

        private IdBrokerMappingSource idBrokerMappingSource;

        private CloudStorageValidation cloudStorageValidation;

        private AwsEnvironmentParameters aws;

        private AzureEnvironmentParametersResponse azure;

        private TagResponse tag;

        private String parentEnvironmentCrn;

        private String parentEnvironmentName;

        private String parentEnvironmentCloudPlatform;

        private ProxyResponse proxyConfig;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
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

        public Builder withCreator(String creator) {
            this.creator = creator;
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

        public Builder withFreeIpa(FreeIpaResponse freeIpa) {
            this.freeIpa = freeIpa;
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

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return this;
        }

        public Builder withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
            this.cloudStorageValidation = cloudStorageValidation;
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

        public Builder withAzure(AzureEnvironmentParametersResponse azure) {
            this.azure = azure;
            return this;
        }

        public Builder withTag(TagResponse tag) {
            this.tag = tag;
            return this;
        }

        public Builder withParentEnvironmentCrn(String parentEnvironmentCrn) {
            this.parentEnvironmentCrn = parentEnvironmentCrn;
            return this;
        }

        public Builder withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return this;
        }

        public Builder withParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
            this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
            return this;
        }

        public Builder withProxyConfig(ProxyResponse proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public DetailedEnvironmentResponse build() {
            DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
            detailedEnvironmentResponse.setCrn(crn);
            detailedEnvironmentResponse.setName(name);
            detailedEnvironmentResponse.setCreator(creator);
            detailedEnvironmentResponse.setDescription(description);
            detailedEnvironmentResponse.setRegions(regions);
            detailedEnvironmentResponse.setCloudPlatform(cloudPlatform);
            detailedEnvironmentResponse.setCredential(credential);
            detailedEnvironmentResponse.setLocation(location);
            detailedEnvironmentResponse.setNetwork(network);
            detailedEnvironmentResponse.setCreateFreeIpa(createFreeIpa);
            detailedEnvironmentResponse.setFreeIpa(freeIpa);
            detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            detailedEnvironmentResponse.setStatusReason(statusReason);
            detailedEnvironmentResponse.setCreated(created);
            detailedEnvironmentResponse.setAuthentication(authentication);
            detailedEnvironmentResponse.setTelemetry(telemetry);
            detailedEnvironmentResponse.setSecurityAccess(securityAccess);
            detailedEnvironmentResponse.setTunnel(tunnel);
            detailedEnvironmentResponse.setIdBrokerMappingSource(idBrokerMappingSource);
            detailedEnvironmentResponse.setCloudStorageValidation(cloudStorageValidation);
            detailedEnvironmentResponse.setAdminGroupName(adminGroupName);
            detailedEnvironmentResponse.setAws(aws);
            detailedEnvironmentResponse.setAzure(azure);
            detailedEnvironmentResponse.setTags(tag);
            detailedEnvironmentResponse.setParentEnvironmentCrn(parentEnvironmentCrn);
            detailedEnvironmentResponse.setParentEnvironmentName(parentEnvironmentName);
            detailedEnvironmentResponse.setParentEnvironmentCloudPlatform(parentEnvironmentCloudPlatform);
            detailedEnvironmentResponse.setProxyConfig(proxyConfig);
            return detailedEnvironmentResponse;
        }
    }
}
