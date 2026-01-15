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

    public static class Builder<T extends DetailedEnvironmentResponse, B extends Builder<T, B>> {
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

        protected Builder() {
        }

        public B withCrn(String crn) {
            this.crn = crn;
            return (B) this;
        }

        public B withName(String name) {
            this.name = name;
            return (B) this;
        }

        public B withOriginalName(String originalName) {
            this.originalName = originalName;
            return (B) this;
        }

        public B withDescription(String description) {
            this.description = description;
            return (B) this;
        }

        public B withRegions(CompactRegionResponse regions) {
            this.regions = regions;
            return (B) this;
        }

        /**
         * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
         * and can become invalid, usage of it can be error-prone
         */
        @Deprecated
        public B withCreator(String creator) {
            this.creator = creator;
            return (B) this;
        }

        public B withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return (B) this;
        }

        public B withCredential(CredentialResponse credential) {
            this.credential = credential;
            return (B) this;
        }

        public B withLocation(LocationResponse location) {
            this.location = location;
            return (B) this;
        }

        public B withNetwork(EnvironmentNetworkResponse network) {
            this.network = network;
            return (B) this;
        }

        public B withTelemetry(TelemetryResponse telemetry) {
            this.telemetry = telemetry;
            return (B) this;
        }

        public B withBackup(BackupResponse backup) {
            this.backup = backup;
            return (B) this;
        }

        public B withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return (B) this;
        }

        public B withCreateFreeIpa(boolean createFreeIpa) {
            this.createFreeIpa = createFreeIpa;
            return (B) this;
        }

        public B withFreeIpa(FreeIpaResponse freeIpa) {
            this.freeIpa = freeIpa;
            return (B) this;
        }

        public B withStatusReason(String statusReason) {
            this.statusReason = statusReason;
            return (B) this;
        }

        public B withCreated(Long created) {
            this.created = created;
            return (B) this;
        }

        public B withAuthentication(EnvironmentAuthenticationResponse authentication) {
            this.authentication = authentication;
            return (B) this;
        }

        public B withSecurityAccess(SecurityAccessResponse securityAccess) {
            this.securityAccess = securityAccess;
            return (B) this;
        }

        public B withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return (B) this;
        }

        public B withIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
            this.idBrokerMappingSource = idBrokerMappingSource;
            return (B) this;
        }

        public B withCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
            this.cloudStorageValidation = cloudStorageValidation;
            return (B) this;
        }

        public B withAdminGroupName(String adminGroupName) {
            this.adminGroupName = adminGroupName;
            return (B) this;
        }

        public B withAws(AwsEnvironmentParameters aws) {
            this.aws = aws;
            return (B) this;
        }

        public B withAzure(AzureEnvironmentParameters azure) {
            this.azure = azure;
            return (B) this;
        }

        public B withYarn(YarnEnvironmentParameters yarn) {
            this.yarn = yarn;
            return (B) this;
        }

        public B withTag(TagResponse tag) {
            this.tag = tag;
            return (B) this;
        }

        public B withParentEnvironmentCrn(String parentEnvironmentCrn) {
            this.parentEnvironmentCrn = parentEnvironmentCrn;
            return (B) this;
        }

        public B withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return (B) this;
        }

        public B withParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
            this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
            return (B) this;
        }

        public B withProxyConfig(ProxyResponse proxyConfig) {
            this.proxyConfig = proxyConfig;
            return (B) this;
        }

        public B withGcp(GcpEnvironmentParameters gcp) {
            this.gcp = gcp;
            return (B) this;
        }

        public B withEnvironmentServiceVersion(String environmentServiceVersion) {
            this.environmentServiceVersion = environmentServiceVersion;
            return (B) this;
        }

        public B withAccountId(String accountId) {
            this.accountId = accountId;
            return (B) this;
        }

        public B withCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
            this.ccmV2TlsType = ccmV2TlsType;
            return (B) this;
        }

        public B withDeletionType(EnvironmentDeletionType deletionType) {
            this.deletionType = deletionType;
            return (B) this;
        }

        public B withEnvironmentDomain(String environmentDomainName) {
            environmentDomain = environmentDomainName;
            return (B) this;
        }

        public B withDataServices(DataServicesResponse dataServices) {
            this.dataServices = dataServices;
            return (B) this;
        }

        public B withEnableSecretEncryption(boolean enableSecretEncryption) {
            this.enableSecretEncryption = enableSecretEncryption;
            return (B) this;
        }

        public B withExternalizedComputeCluster(ExternalizedComputeClusterResponse externalizedComputeCluster) {
            this.externalizedComputeCluster = externalizedComputeCluster;
            return (B) this;
        }

        public B withEnableComputeCluster(boolean enableComputeCluster) {
            this.enableComputeCluster = enableComputeCluster;
            return (B) this;
        }

        public B withEnvironmentType(String environmentType) {
            this.environmentType = environmentType;
            return (B) this;
        }

        public B withRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
            this.remoteEnvironmentCrn = remoteEnvironmentCrn;
            return (B) this;
        }

        public B withEncryptionProfileCrn(String encryptionProfileCrn) {
            this.encryptionProfileCrn = encryptionProfileCrn;
            return (B) this;
        }

        public B withNotificationParameters(NotificationParameters notificationParameters) {
            this.notificationParameters = notificationParameters;
            return (B) this;
        }

        public T build() {
            return build((T) new DetailedEnvironmentResponse());
        }

        protected T build(T detailedEnvironmentResponse) {
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
