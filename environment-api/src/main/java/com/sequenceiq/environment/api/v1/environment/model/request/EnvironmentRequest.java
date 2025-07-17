package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.ValidEnvironmentName;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentRequest extends EnvironmentBaseRequest implements CredentialAwareEnvRequest, TaggableRequest {

    static final String LENGHT_INVALID_MSG = "The length of the environments's name has to be in range of 5 to 28";

    @Size(max = 28, min = 5, message = LENGHT_INVALID_MSG)
    @ValidEnvironmentName
    @Schema(description = ModelDescriptions.NAME, required = true)
    @NotNull
    private String name;

    @Size(max = 1000)
    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    /**
     * This field is not used anymore
     *
     * @deprecated As of Environment service 2.27, because this is duplication
     * {@link #location} instead.
     */
    @Schema(description = EnvironmentModelDescription.REGIONS)
    @Deprecated(forRemoval = true)
    private Set<String> regions = new HashSet<>();

    @Schema(description = EnvironmentModelDescription.LOCATION, required = true)
    @NotNull
    private LocationRequest location;

    @Valid
    @Schema(description = EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkRequest network;

    @Schema(description = EnvironmentModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @Schema(description = EnvironmentModelDescription.BACKUP)
    private @Valid BackupRequest backup;

    @Valid
    @Schema(description = EnvironmentModelDescription.AUTHENTICATION)
    private EnvironmentAuthenticationRequest authentication;

    @Valid
    @Schema(description = EnvironmentModelDescription.FREE_IPA)
    private AttachedFreeIpaRequest freeIpa;

    @Valid
    @Schema(description = EnvironmentModelDescription.EXTERNALIZED_COMPUTE_CLUSTER)
    private ExternalizedComputeCreateRequest externalizedComputeCreateRequest;

    @Valid
    @Schema(description = EnvironmentModelDescription.SECURITY_ACCESS)
    private SecurityAccessRequest securityAccess;

    @Schema(description = EnvironmentModelDescription.TUNNEL)
    private Tunnel tunnel;

    @Schema(description = EnvironmentModelDescription.OVERRIDE_TUNNEL)
    private Boolean overrideTunnel;

    @Schema(description = EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource = IdBrokerMappingSource.IDBMMS;

    @Schema(description = EnvironmentModelDescription.CLOUD_STORAGE_VALIDATION)
    private CloudStorageValidation cloudStorageValidation = CloudStorageValidation.ENABLED;

    @Schema(description = EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    @Schema(description = EnvironmentModelDescription.PROXYCONFIG_NAME)
    private String proxyConfigName;

    @Valid
    @Schema(description = EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    @Valid
    @Schema(description = EnvironmentModelDescription.AZURE_PARAMETERS)
    private AzureEnvironmentParameters azure;

    @Valid
    @Schema(description = EnvironmentModelDescription.TAGS)
    private Map<String, String> tags = new HashMap<>();

    @Schema(description = EnvironmentModelDescription.PARENT_ENVIRONMENT_NAME)
    private String parentEnvironmentName;

    @Valid
    @Schema(description = EnvironmentModelDescription.GCP_PARAMETERS)
    private GcpEnvironmentParameters gcp;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_SERVICE_VERSION)
    private String environmentServiceVersion;

    private CcmV2TlsType ccmV2TlsType;

    @Valid
    @Schema(description = EnvironmentModelDescription.DATA_SERVICES)
    private DataServicesRequest dataServices;

    @Schema(description = EnvironmentModelDescription.ENVIRONMENT_TYPE)
    private String environmentType;

    @Schema(description = EnvironmentModelDescription.ENCRYPTION_PROFILE_NAME)
    private String encryptionProfileName;

    public AttachedFreeIpaRequest getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(AttachedFreeIpaRequest freeIpa) {
        this.freeIpa = freeIpa;
    }

    public ExternalizedComputeCreateRequest getExternalizedComputeCreateRequest() {
        return externalizedComputeCreateRequest;
    }

    public void setExternalizedComputeCreateRequest(ExternalizedComputeCreateRequest externalizedComputeCreateRequest) {
        this.externalizedComputeCreateRequest = externalizedComputeCreateRequest;
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

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public LocationRequest getLocation() {
        return location;
    }

    public void setLocation(LocationRequest location) {
        this.location = location;
    }

    public TelemetryRequest getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryRequest telemetry) {
        this.telemetry = telemetry;
    }

    public BackupRequest getBackup() {
        return backup;
    }

    public void setBackup(BackupRequest backup) {
        this.backup = backup;
    }

    public EnvironmentNetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkRequest network) {
        this.network = network;
    }

    public EnvironmentAuthenticationRequest getAuthentication() {
        return authentication;
    }

    public void setAuthentication(EnvironmentAuthenticationRequest authentication) {
        this.authentication = authentication;
    }

    public SecurityAccessRequest getSecurityAccess() {
        return securityAccess;
    }

    public void setSecurityAccess(SecurityAccessRequest securityAccess) {
        this.securityAccess = securityAccess;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public Boolean getOverrideTunnel() {
        return overrideTunnel;
    }

    public void setOverrideTunnel(Boolean overrideTunnel) {
        this.overrideTunnel = overrideTunnel;
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

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public void addTag(String key, String value) {
        tags.put(key, value);
    }

    public String getParentEnvironmentName() {
        return parentEnvironmentName;
    }

    public void setParentEnvironmentName(String parentEnvironmentName) {
        this.parentEnvironmentName = parentEnvironmentName;
    }

    public String getProxyConfigName() {
        return proxyConfigName;
    }

    public void setProxyConfigName(String proxyConfigName) {
        this.proxyConfigName = proxyConfigName;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions == null ? new HashSet<>() : regions;
    }

    public String getEnvironmentServiceVersion() {
        return environmentServiceVersion;
    }

    public void setEnvironmentServiceVersion(String environmentServiceVersion) {
        this.environmentServiceVersion = environmentServiceVersion;
    }

    public GcpEnvironmentParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpEnvironmentParameters gcp) {
        this.gcp = gcp;
    }

    public CcmV2TlsType getCcmV2TlsType() {
        return ccmV2TlsType;
    }

    public void setCcmV2TlsType(CcmV2TlsType ccmV2TlsType) {
        this.ccmV2TlsType = ccmV2TlsType;
    }

    public DataServicesRequest getDataServices() {
        return dataServices;
    }

    public void setDataServices(DataServicesRequest dataServices) {
        this.dataServices = dataServices;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(String environmentType) {
        this.environmentType = environmentType;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    public void setEncryptionProfileName(String encryptionProfileName) {
        this.encryptionProfileName = encryptionProfileName;
    }

    @Override
    public String toString() {
        return "EnvironmentRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", credentialName='" + credentialName + '\'' +
                ", regions=" + regions +
                ", location=" + location +
                ", network=" + network +
                ", telemetry=" + telemetry +
                ", backup=" + backup +
                ", authentication=" + authentication +
                ", freeIpa=" + freeIpa +
                ", externalizedCluster=" + externalizedComputeCreateRequest +
                ", securityAccess=" + securityAccess +
                ", tunnel=" + tunnel +
                ", overrideTunnel=" + overrideTunnel +
                ", idBrokerMappingSource=" + idBrokerMappingSource +
                ", cloudStorageValidation=" + cloudStorageValidation +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", proxyConfigName='" + proxyConfigName + '\'' +
                ", aws=" + aws +
                ", azure=" + azure +
                ", gcp=" + gcp +
                ", tags=" + tags +
                ", parentEnvironmentName='" + parentEnvironmentName + '\'' +
                ", environmentServiceVersion='" + environmentServiceVersion + '\'' +
                ", ccmV2TlsType=" + ccmV2TlsType +
                ", dataServices=" + dataServices +
                ", environmentType=" + environmentType +
                "} " + super.toString();
    }
}
