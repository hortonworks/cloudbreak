package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Optional;

import javax.validation.Valid;

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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(subTypes = {DetailedEnvironmentResponse.class, SimpleEnvironmentResponse.class})
public abstract class EnvironmentBaseResponse implements TaggedResponse {
    @ApiModelProperty(ModelDescriptions.ID)
    private String crn;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(ModelDescriptions.CREATOR)
    private String creator;

    @ApiModelProperty(EnvironmentModelDescription.CREATE_FREEIPA)
    private Boolean createFreeIpa = Boolean.TRUE;

    @ApiModelProperty(EnvironmentModelDescription.FREEIPA)
    private FreeIpaResponse freeIpa;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private CompactRegionResponse regions;

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private LocationResponse location;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
    private TelemetryResponse telemetry;

    @ApiModelProperty(EnvironmentModelDescription.BACKUP)
    private @Valid BackupResponse backup;

    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkResponse network;

    @ApiModelProperty(EnvironmentModelDescription.STATUS)
    private EnvironmentStatus environmentStatus;

    @ApiModelProperty(EnvironmentModelDescription.AUTHENTICATION)
    private EnvironmentAuthenticationResponse authentication;

    private String statusReason;

    private Long created;

    @ApiModelProperty(EnvironmentModelDescription.TUNNEL)
    private Tunnel tunnel;

    @ApiModelProperty(EnvironmentModelDescription.SECURITY_ACCESS)
    private SecurityAccessResponse securityAccess;

    @ApiModelProperty(EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource;

    @ApiModelProperty(EnvironmentModelDescription.CLOUD_STORAGE_VALIDATION)
    private CloudStorageValidation cloudStorageValidation;

    @ApiModelProperty(EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    @ApiModelProperty(EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    @ApiModelProperty(EnvironmentModelDescription.AZURE_PARAMETERS)
    private AzureEnvironmentParameters azure;

    @ApiModelProperty(EnvironmentModelDescription.TAGS)
    private TagResponse tags;

    @ApiModelProperty(EnvironmentModelDescription.PARENT_ENVIRONMENT_CRN)
    private String parentEnvironmentCrn;

    @ApiModelProperty(EnvironmentModelDescription.PARENT_ENVIRONMENT_NAME)
    private String parentEnvironmentName;

    @ApiModelProperty(EnvironmentModelDescription.PARENT_ENVIRONMENT_CLOUD_PLATFORM)
    private String parentEnvironmentCloudPlatform;

    @ApiModelProperty(EnvironmentModelDescription.GCP_PARAMETERS)
    private GcpEnvironmentParameters gcp;

    @ApiModelProperty(EnvironmentModelDescription.YARN_PARAMETERS)
    private YarnEnvironmentParameters yarn;

    @ApiModelProperty(EnvironmentModelDescription.ENVIRONMENT_SERVICE_VERSION)
    private String environmentServiceVersion;

    private CcmV2TlsType ccmV2TlsType;

    @JsonIgnore
    public boolean isCloudStorageLoggingEnabled() {
        return telemetry != null && telemetry.getFeatures() != null && telemetry.getFeatures().getCloudStorageLogging() != null
                && telemetry.getFeatures().getCloudStorageLogging().isEnabled();
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

    public String getCreator() {
        return creator;
    }

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

    @Override
    public String toString() {
        return "EnvironmentBaseResponse{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
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
                ", ccmV2TlsType='" + ccmV2TlsType + '\'' +
                '}';
    }
}
