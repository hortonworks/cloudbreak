package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyViewResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "SimpleEnvironmentV1Response")
public class SimpleEnvironmentResponse extends EnvironmentBaseResponse {

    private CredentialViewResponse credential;

    private ProxyViewResponse proxyConfig;

    public static Builder builder() {
        return new Builder();
    }

    public CredentialViewResponse getCredential() {
        return credential;
    }

    public void setCredential(CredentialViewResponse credential) {
        this.credential = credential;
    }

    public ProxyViewResponse getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyViewResponse proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public String toString() {
        return super.toString() + "SimpleEnvironmentResponse{" +
                "credential=" + credential +
                ", proxyConfig=" + proxyConfig +
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

        private CredentialViewResponse credentialViewResponse;

        private LocationResponse location;

        private TelemetryResponse telemetry;

        private BackupResponse backup;

        private EnvironmentNetworkResponse network;

        private EnvironmentStatus environmentStatus;

        private String statusReason;

        private Long created;

        private Tunnel tunnel;

        private String adminGroupName;

        private AwsEnvironmentParameters aws;

        private TagResponse tags;

        private String parentEnvironmentName;

        private ProxyViewResponse proxyConfig;

        private AzureEnvironmentParameters azure;

        private GcpEnvironmentParameters gcp;

        private YarnEnvironmentParameters yarn;

        private String environmentServiceVersion;

        private CcmV2TlsType ccmV2TlsType;

        private EnvironmentDeletionType deletionType;

        private String environmentDomain;

        private DataServicesResponse dataServices;

        private boolean enableSecretEncryption;

        private boolean enableComputeCluster;

        private String environmentType;

        private String remoteEnvironmentCrn;

        private EncryptionProfileResponse encryptionProfile;

        private Builder() {
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withOriginalName(String originalName) {
            this.originalName = originalName;
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
            credentialViewResponse = credentialResponse;
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

        public Builder withTelemetry(TelemetryResponse telemetry) {
            this.telemetry = telemetry;
            return this;
        }

        public Builder withBackup(BackupResponse backup) {
            this.backup = backup;
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

        public Builder withAzure(AzureEnvironmentParameters azure) {
            this.azure = azure;
            return this;
        }

        public Builder withYarn(YarnEnvironmentParameters yarn) {
            this.yarn = yarn;
            return this;
        }

        public Builder withTag(TagResponse tags) {
            this.tags = tags;
            return this;
        }

        public Builder withParentEnvironmentName(String parentEnvironmentName) {
            this.parentEnvironmentName = parentEnvironmentName;
            return this;
        }

        public Builder withProxyConfig(ProxyViewResponse proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder withGcp(GcpEnvironmentParameters gcp) {
            this.gcp = gcp;
            return this;
        }

        public Builder withDeletionType(EnvironmentDeletionType deletionType) {
            this.deletionType = deletionType;
            return this;
        }

        public Builder withEnvironmentServiceVersion(String environmentServiceVersion) {
            this.environmentServiceVersion = environmentServiceVersion;
            return this;
        }

        public Builder withCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
            this.ccmV2TlsType = ccmV2TlsType;
            return this;
        }

        public Builder withEnvironmentDomain(String environmentDomain) {
            this.environmentDomain = environmentDomain;
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

        public Builder withEncryptionProfile(EncryptionProfileResponse encryptionProfile) {
            this.encryptionProfile = encryptionProfile;
            return this;
        }

        public SimpleEnvironmentResponse build() {
            SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();
            simpleEnvironmentResponse.setCrn(crn);
            simpleEnvironmentResponse.setName(name);
            simpleEnvironmentResponse.setOriginalName(originalName);
            simpleEnvironmentResponse.setCreator(creator);
            simpleEnvironmentResponse.setDescription(description);
            simpleEnvironmentResponse.setRegions(regions);
            simpleEnvironmentResponse.setCloudPlatform(cloudPlatform);
            simpleEnvironmentResponse.setCredential(credentialViewResponse);
            simpleEnvironmentResponse.setLocation(location);
            simpleEnvironmentResponse.setNetwork(network);
            simpleEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            simpleEnvironmentResponse.setCreateFreeIpa(createFreeIpa);
            simpleEnvironmentResponse.setFreeIpa(freeIpa);
            simpleEnvironmentResponse.setStatusReason(statusReason);
            simpleEnvironmentResponse.setCreated(created);
            simpleEnvironmentResponse.setTelemetry(telemetry);
            simpleEnvironmentResponse.setBackup(backup);
            simpleEnvironmentResponse.setTunnel(tunnel);
            simpleEnvironmentResponse.setAws(aws);
            simpleEnvironmentResponse.setAzure(azure);
            simpleEnvironmentResponse.setGcp(gcp);
            simpleEnvironmentResponse.setYarn(yarn);
            simpleEnvironmentResponse.setAdminGroupName(adminGroupName);
            simpleEnvironmentResponse.setTags(tags);
            simpleEnvironmentResponse.setParentEnvironmentName(parentEnvironmentName);
            simpleEnvironmentResponse.setProxyConfig(proxyConfig);
            simpleEnvironmentResponse.setEnvironmentServiceVersion(environmentServiceVersion);
            simpleEnvironmentResponse.setCcmV2TlsType(ccmV2TlsType);
            simpleEnvironmentResponse.setDeletionType(deletionType);
            simpleEnvironmentResponse.setEnvironmentDomain(environmentDomain);
            simpleEnvironmentResponse.setDataServices(dataServices);
            simpleEnvironmentResponse.setEnableSecretEncryption(enableSecretEncryption);
            simpleEnvironmentResponse.setEnableComputeCluster(enableComputeCluster);
            simpleEnvironmentResponse.setEnvironmentType(environmentType);
            simpleEnvironmentResponse.setRemoteEnvironmentCrn(remoteEnvironmentCrn);
            simpleEnvironmentResponse.setEncryptionProfile(encryptionProfile);
            return simpleEnvironmentResponse;
        }
    }
}
