package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.NotificationParameters;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "DetailedEnvironmentV1Response")
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    @Schema(description = EnvironmentModelDescription.CREDENTIAL_RESPONSE)
    private CredentialResponse credential;

    @Schema(description = EnvironmentModelDescription.PROXYCONFIG_RESPONSE)
    private ProxyResponse proxyConfig;

    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_CLUSTER)
    private ExternalizedComputeClusterResponse externalizedComputeCluster;

    public static Builder builder() {
        return new Builder();
    }

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

    public ExternalizedComputeClusterResponse getExternalizedComputeCluster() {
        return externalizedComputeCluster;
    }

    public void setExternalizedComputeCluster(ExternalizedComputeClusterResponse externalizedComputeCluster) {
        this.externalizedComputeCluster = externalizedComputeCluster;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "DetailedEnvironmentResponse{" +
                "credential=" + credential +
                ", proxyConfig=" + proxyConfig +
                ", externalizedComputeCluster=" + externalizedComputeCluster +
                '}';
    }

    public static final class Builder {
        private String crn;

        private String name;

        private String originalName;

        private String description;

        /**
         * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
         * and can become invalid, usage of it can be error-prone
         */
        @Deprecated
        private String creator;

        private boolean createFreeIpa = true;

        private FreeIpaResponse freeIpa;

        private CompactRegionResponse regions;

        private String cloudPlatform;

        private CredentialResponse credential;

        private LocationResponse location;

        private EnvironmentNetworkResponse network;

        private TelemetryResponse telemetry;

        private BackupResponse backup;

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

        private AzureEnvironmentParameters azure;

        private TagResponse tag;

        private String parentEnvironmentCrn;

        private String parentEnvironmentName;

        private String parentEnvironmentCloudPlatform;

        private ProxyResponse proxyConfig;

        private GcpEnvironmentParameters gcp;

        private YarnEnvironmentParameters yarn;

        private String environmentServiceVersion;

        private CcmV2TlsType ccmV2TlsType;

        private EnvironmentDeletionType deletionType;

        private String environmentDomain;

        private String accountId;

        private DataServicesResponse dataServices;

        private boolean enableSecretEncryption;

        private ExternalizedComputeClusterResponse externalizedComputeCluster;

        private boolean enableComputeCluster;

        private String environmentType;

        private String remoteEnvironmentCrn;

        private String encryptionProfileCrn;

        private NotificationParameters notificationParameters;

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

        public Builder withOriginalName(String originalName) {
            this.originalName = originalName;
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

        /**
         * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
         * and can become invalid, usage of it can be error-prone
         */
        @Deprecated
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

        public Builder withBackup(BackupResponse backup) {
            this.backup = backup;
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

        public Builder withAzure(AzureEnvironmentParameters azure) {
            this.azure = azure;
            return this;
        }

        public Builder withYarn(YarnEnvironmentParameters yarn) {
            this.yarn = yarn;
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

        public Builder withGcp(GcpEnvironmentParameters gcp) {
            this.gcp = gcp;
            return this;
        }

        public Builder withEnvironmentServiceVersion(String environmentServiceVersion) {
            this.environmentServiceVersion = environmentServiceVersion;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
            this.ccmV2TlsType = ccmV2TlsType;
            return this;
        }

        public Builder withDeletionType(EnvironmentDeletionType deletionType) {
            this.deletionType = deletionType;
            return this;
        }

        public Builder withEnvironmentDomain(String environmentDomainName) {
            environmentDomain = environmentDomainName;
            return this;
        }

        public Builder withDataServices(DataServicesResponse dataServices) {
            this.dataServices = dataServices;
            return this;
        }

        public Builder withEnableSecretEncryption(boolean enableSecretEncryption) {
            this.enableSecretEncryption = enableSecretEncryption;
            return this;
        }

        public Builder withExternalizedComputeCluster(ExternalizedComputeClusterResponse externalizedComputeCluster) {
            this.externalizedComputeCluster = externalizedComputeCluster;
            return this;
        }

        public Builder withEnableComputeCluster(boolean enableComputeCluster) {
            this.enableComputeCluster = enableComputeCluster;
            return this;
        }

        public Builder withEnvironmentType(String environmentType) {
            this.environmentType = environmentType;
            return this;
        }

        public Builder withRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
            this.remoteEnvironmentCrn = remoteEnvironmentCrn;
            return this;
        }

        public Builder withEncryptionProfileCrn(String encryptionProfileCrn) {
            this.encryptionProfileCrn = encryptionProfileCrn;
            return this;
        }

        public Builder withNotificationParameters(NotificationParameters notificationParameters) {
            this.notificationParameters = notificationParameters;
            return this;
        }

        public DetailedEnvironmentResponse build() {
            DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
            detailedEnvironmentResponse.setCrn(crn);
            detailedEnvironmentResponse.setName(name);
            detailedEnvironmentResponse.setOriginalName(originalName);
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
            detailedEnvironmentResponse.setBackup(backup);
            detailedEnvironmentResponse.setSecurityAccess(securityAccess);
            detailedEnvironmentResponse.setTunnel(tunnel);
            detailedEnvironmentResponse.setIdBrokerMappingSource(idBrokerMappingSource);
            detailedEnvironmentResponse.setCloudStorageValidation(cloudStorageValidation);
            detailedEnvironmentResponse.setAdminGroupName(adminGroupName);
            detailedEnvironmentResponse.setAws(aws);
            detailedEnvironmentResponse.setTags(tag);
            detailedEnvironmentResponse.setParentEnvironmentCrn(parentEnvironmentCrn);
            detailedEnvironmentResponse.setParentEnvironmentName(parentEnvironmentName);
            detailedEnvironmentResponse.setParentEnvironmentCloudPlatform(parentEnvironmentCloudPlatform);
            detailedEnvironmentResponse.setProxyConfig(proxyConfig);
            detailedEnvironmentResponse.setAzure(azure);
            detailedEnvironmentResponse.setGcp(gcp);
            detailedEnvironmentResponse.setYarn(yarn);
            detailedEnvironmentResponse.setEnvironmentServiceVersion(environmentServiceVersion);
            detailedEnvironmentResponse.setCcmV2TlsType(ccmV2TlsType);
            detailedEnvironmentResponse.setDeletionType(deletionType);
            detailedEnvironmentResponse.setEnvironmentDomain(environmentDomain);
            detailedEnvironmentResponse.setAccountId(accountId);
            detailedEnvironmentResponse.setDataServices(dataServices);
            detailedEnvironmentResponse.setEnableSecretEncryption(enableSecretEncryption);
            detailedEnvironmentResponse.setExternalizedComputeCluster(externalizedComputeCluster);
            detailedEnvironmentResponse.setEnableComputeCluster(enableComputeCluster);
            detailedEnvironmentResponse.setEnvironmentType(environmentType);
            detailedEnvironmentResponse.setRemoteEnvironmentCrn(remoteEnvironmentCrn);
            detailedEnvironmentResponse.setEncryptionProfileCrn(encryptionProfileCrn);
            detailedEnvironmentResponse.setNotificationParameters(notificationParameters);
            return detailedEnvironmentResponse;
        }
    }
}
