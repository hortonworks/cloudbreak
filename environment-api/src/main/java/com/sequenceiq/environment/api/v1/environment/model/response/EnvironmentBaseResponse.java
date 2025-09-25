package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Optional;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.yarn.YarnEnvironmentParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(subTypes = {DetailedEnvironmentResponse.class, SimpleEnvironmentResponse.class})
public abstract class EnvironmentBaseResponse implements TaggedResponse {
    @Schema(description = ModelDescriptions.ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = ModelDescriptions.ORIGINAL_NAME)
    private String originalName;

    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = EnvironmentModelDescription.CLOUD_PLATFORM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String cloudPlatform;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Schema(description = ModelDescriptions.CREATOR)
    private String creator;

    @Schema(description = EnvironmentModelDescription.CREATE_FREEIPA, requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean createFreeIpa = Boolean.TRUE;

    @Schema(description = EnvironmentModelDescription.FREEIPA)
    private FreeIpaResponse freeIpa;

    @Schema(description = EnvironmentModelDescription.REGIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private CompactRegionResponse regions;

    @Schema(description = EnvironmentModelDescription.LOCATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private LocationResponse location;

    @Schema(description = EnvironmentModelDescription.TELEMETRY)
    private TelemetryResponse telemetry;

    @Schema(description = EnvironmentModelDescription.BACKUP)
    private @Valid BackupResponse backup;

    @Schema(description = EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkResponse network;

    @Schema(description = EnvironmentModelDescription.STATUS)
    private EnvironmentStatus environmentStatus;

    @Schema(description = EnvironmentModelDescription.AUTHENTICATION)
    private EnvironmentAuthenticationResponse authentication;

    private String statusReason;

    private Long created;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountId;

    @Schema(description = EnvironmentModelDescription.TUNNEL)
    private Tunnel tunnel;

    @Schema(description = EnvironmentModelDescription.SECURITY_ACCESS)
    private SecurityAccessResponse securityAccess;

    @Schema(description = EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource;

    @Schema(description = EnvironmentModelDescription.CLOUD_STORAGE_VALIDATION)
    private CloudStorageValidation cloudStorageValidation;

    @Schema(description = EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    @Schema(description = EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    @Schema(description = EnvironmentModelDescription.AZURE_PARAMETERS)
    private AzureEnvironmentParameters azure;

    @Schema(description = EnvironmentModelDescription.TAGS)
    private TagResponse tags;

    @Schema(description = EnvironmentModelDescription.PARENT_ENVIRONMENT_CRN)
    private String parentEnvironmentCrn;

    @Schema(description = EnvironmentModelDescription.PARENT_ENVIRONMENT_NAME)
    private String parentEnvironmentName;

    @Schema(description = EnvironmentModelDescription.PARENT_ENVIRONMENT_CLOUD_PLATFORM)
    private String parentEnvironmentCloudPlatform;

    @Schema(description = EnvironmentModelDescription.GCP_PARAMETERS)
    private GcpEnvironmentParameters gcp;

    @Schema(description = EnvironmentModelDescription.YARN_PARAMETERS)
    private YarnEnvironmentParameters yarn;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_SERVICE_VERSION)
    private String environmentServiceVersion;

    private CcmV2TlsType ccmV2TlsType;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_DELETION_TYPE)
    private EnvironmentDeletionType deletionType;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_DOMAIN_NAME)
    private String environmentDomain;

    @Schema(description = EnvironmentModelDescription.DATA_SERVICES)
    private DataServicesResponse dataServices;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_ENABLE_SECRET_ENCRYPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableSecretEncryption;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_ENABLE_COMPUTE_CLUSTER, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enableComputeCluster;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_TYPE,
            subTypes = {DetailedEnvironmentResponse.class, SimpleEnvironmentResponse.class},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentType;

    @Schema(description = EnvironmentModelDescription.REMOTE_ENV_CRN, subTypes = {DetailedEnvironmentResponse.class, SimpleEnvironmentResponse.class})
    private String remoteEnvironmentCrn;

    @Schema(description = EnvironmentModelDescription.ENCRYPTION_PROFILE)
    private String encryptionProfileName;

    @JsonIgnore
    public boolean isCloudStorageLoggingEnabled() {
        return telemetry != null && telemetry.getFeatures() != null && telemetry.getFeatures().getCloudStorageLogging() != null
                && telemetry.getFeatures().getCloudStorageLogging().getEnabled();
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CompactRegionResponse getRegions() {
        return regions;
    }

    public void setRegions(CompactRegionResponse regions) {
        this.regions = regions;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public LocationResponse getLocation() {
        return location;
    }

    public void setLocation(LocationResponse location) {
        this.location = location;
    }

    public TelemetryResponse getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryResponse telemetry) {
        this.telemetry = telemetry;
    }

    public BackupResponse getBackup() {
        return backup;
    }

    public void setBackup(BackupResponse backup) {
        this.backup = backup;
    }

    public String getBackupLocation() {
        return backup != null ? backup.getStorageLocation() : null;
    }

    public String getBackupInstanceProfile() {
        return backup != null ? backup.getInstanceProfile() : null;
    }

    public EnvironmentNetworkResponse getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkResponse network) {
        this.network = network;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    public void setEnvironmentStatus(EnvironmentStatus environmentStatus) {
        this.environmentStatus = environmentStatus;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Boolean getCreateFreeIpa() {
        return createFreeIpa;
    }

    public void setCreateFreeIpa(Boolean createFreeIpa) {
        this.createFreeIpa = createFreeIpa;
    }

    public FreeIpaResponse getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(FreeIpaResponse freeIpa) {
        this.freeIpa = freeIpa;
    }

    public EnvironmentAuthenticationResponse getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthenticationResponse authentication) {
        this.authentication = authentication;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public SecurityAccessResponse getSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(SecurityAccessResponse securityAccess) {
        this.securityAccess = securityAccess;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public IdBrokerMappingSource getIdBrokerMappingSource() {
        return idBrokerMappingSource;
    }

    public void setIdBrokerMappingSource(IdBrokerMappingSource idBrokerMappingSource) {
        this.idBrokerMappingSource = idBrokerMappingSource;
    }

    public CloudStorageValidation getCloudStorageValidation() {
        return cloudStorageValidation;
    }

    public void setCloudStorageValidation(CloudStorageValidation cloudStorageValidation) {
        this.cloudStorageValidation = cloudStorageValidation;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public AwsEnvironmentParameters getAws() {
        return aws;
    }

    public void setAws(AwsEnvironmentParameters aws) {
        this.aws = aws;
    }

    public AzureEnvironmentParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureEnvironmentParameters azure) {
        this.azure = azure;
    }

    public TagResponse getTags() {
        return tags;
    }

    public void setTags(TagResponse tags) {
        this.tags = tags;
    }

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(tags)
                .map(t -> t.getTagValue(key))
                .orElse(null);
    }

    public String getParentEnvironmentCrn() {
        return parentEnvironmentCrn;
    }

    public void setParentEnvironmentCrn(String parentEnvironmentCrn) {
        this.parentEnvironmentCrn = parentEnvironmentCrn;
    }

    public String getParentEnvironmentName() {
        return parentEnvironmentName;
    }

    public void setParentEnvironmentName(String parentEnvironmentName) {
        this.parentEnvironmentName = parentEnvironmentName;
    }

    public String getParentEnvironmentCloudPlatform() {
        return parentEnvironmentCloudPlatform;
    }

    public void setParentEnvironmentCloudPlatform(String parentEnvironmentCloudPlatform) {
        this.parentEnvironmentCloudPlatform = parentEnvironmentCloudPlatform;
    }

    public GcpEnvironmentParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpEnvironmentParameters gcp) {
        this.gcp = gcp;
    }

    public YarnEnvironmentParameters getYarn() {
        return yarn;
    }

    public void setYarn(YarnEnvironmentParameters yarn) {
        this.yarn = yarn;
    }

    public String getEnvironmentServiceVersion() {
        return environmentServiceVersion;
    }

    public void setEnvironmentServiceVersion(String environmentServiceVersion) {
        this.environmentServiceVersion = environmentServiceVersion;
    }

    public CcmV2TlsType getCcmV2TlsType() {
        return ccmV2TlsType;
    }

    public void setCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
        this.ccmV2TlsType = ccmV2TlsType;
    }

    public EnvironmentDeletionType getDeletionType() {
        return deletionType;
    }

    public void setDeletionType(EnvironmentDeletionType deletionType) {
        this.deletionType = deletionType;
    }

    public String getEnvironmentDomain() {
        return environmentDomain;
    }

    public void setEnvironmentDomain(String environmentDomain) {
        this.environmentDomain = environmentDomain;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public DataServicesResponse getDataServices() {
        return dataServices;
    }

    public void setDataServices(DataServicesResponse dataServices) {
        this.dataServices = dataServices;
    }

    public boolean isEnableSecretEncryption() {
        return enableSecretEncryption;
    }

    public void setEnableSecretEncryption(boolean enableSecretEncryption) {
        this.enableSecretEncryption = enableSecretEncryption;
    }

    public boolean isEnableComputeCluster() {
        return enableComputeCluster;
    }

    public void setEnableComputeCluster(boolean enableComputeCluster) {
        this.enableComputeCluster = enableComputeCluster;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    public void setEncryptionProfileName(String encryptionProfileName) {
        this.encryptionProfileName = encryptionProfileName;
    }

    @Override
    public String toString() {
        return "EnvironmentBaseResponse{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", originalName='" + originalName + '\'' +
                ", description='" + description + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", creator='" + creator + '\'' +
                ", createFreeIpa=" + createFreeIpa +
                ", freeIpa=" + freeIpa +
                ", regions=" + regions +
                ", location=" + location +
                ", telemetry=" + telemetry +
                ", backup=" + backup +
                ", network=" + network +
                ", environmentStatus=" + environmentStatus +
                ", authentication=" + authentication +
                ", statusReason='" + statusReason + '\'' +
                ", created=" + created +
                ", tunnel=" + tunnel +
                ", securityAccess=" + securityAccess +
                ", idBrokerMappingSource=" + idBrokerMappingSource +
                ", cloudStorageValidation=" + cloudStorageValidation +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", aws=" + aws +
                ", azure=" + azure +
                ", tags=" + tags +
                ", parentEnvironmentCrn='" + parentEnvironmentCrn + '\'' +
                ", parentEnvironmentName='" + parentEnvironmentName + '\'' +
                ", parentEnvironmentCloudPlatform='" + parentEnvironmentCloudPlatform + '\'' +
                ", gcp=" + gcp +
                ", yarn=" + yarn +
                ", environmentServiceVersion='" + environmentServiceVersion + '\'' +
                ", ccmV2TlsType=" + ccmV2TlsType +
                ", deletionType=" + deletionType +
                ", environmentDomain='" + environmentDomain + '\'' +
                ", dataServices=" + dataServices +
                ", enableSecretEncryption=" + enableSecretEncryption +
                ", enableComputeCluster=" + enableComputeCluster +
                ", environmentType=" + environmentType +
                ", remoteEnvironmentCrn=" + remoteEnvironmentCrn +
                ", encryptionProfileName=" + encryptionProfileName +
                '}';
    }
}
