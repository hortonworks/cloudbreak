package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.validation.ValidEnvironmentName;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentV1Request")
public class EnvironmentRequest extends EnvironmentBaseRequest implements CredentialAwareEnvRequest, TaggableRequest {

    static final String LENGHT_INVALID_MSG = "The length of the environments's name has to be in range of 5 to 28";

    @Size(max = 28, min = 5, message = LENGHT_INVALID_MSG)
    @ValidEnvironmentName
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @NotNull
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    /**
     * This field is not used anymore
     * @deprecated As of Environment service 2.27, because this is duplication
     *             {@link #location} instead.
     */
    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    @Deprecated(forRemoval = true)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(value = EnvironmentModelDescription.LOCATION, required = true)
    @NotNull
    private LocationRequest location;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkRequest network;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @ApiModelProperty(EnvironmentModelDescription.BACKUP)
    private @Valid BackupRequest backup;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.AUTHENTICATION)
    private EnvironmentAuthenticationRequest authentication;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.FREE_IPA)
    private AttachedFreeIpaRequest freeIpa;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.SECURITY_ACCESS)
    private SecurityAccessRequest securityAccess;

    @ApiModelProperty(EnvironmentModelDescription.TUNNEL)
    private Tunnel tunnel;

    @ApiModelProperty(EnvironmentModelDescription.IDBROKER_MAPPING_SOURCE)
    private IdBrokerMappingSource idBrokerMappingSource = IdBrokerMappingSource.IDBMMS;

    @ApiModelProperty(EnvironmentModelDescription.CLOUD_STORAGE_VALIDATION)
    private CloudStorageValidation cloudStorageValidation = CloudStorageValidation.ENABLED;

    @ApiModelProperty(EnvironmentModelDescription.ADMIN_GROUP_NAME)
    private String adminGroupName;

    @ApiModelProperty(EnvironmentModelDescription.PROXYCONFIG_NAME)
    private String proxyConfigName;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.AWS_PARAMETERS)
    private AwsEnvironmentParameters aws;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.AZURE_PARAMETERS)
    private AzureEnvironmentParameters azure;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.TAGS)
    private Map<String, String> tags = new HashMap<>();

    @ApiModelProperty(value = EnvironmentModelDescription.PARENT_ENVIRONMENT_NAME)
    private String parentEnvironmentName;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.GCP_PARAMETERS)
    private GcpEnvironmentParameters gcp;

    @ApiModelProperty(EnvironmentModelDescription.ENVIRONMENT_SERVICE_VERSION)
    private String environmentServiceVersion;

    public AttachedFreeIpaRequest getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(AttachedFreeIpaRequest freeIpa) {
        this.freeIpa = freeIpa;
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
                ", backupRequest=" + backup +
                ", authentication=" + authentication +
                ", freeIpa=" + freeIpa +
                ", securityAccess=" + securityAccess +
                ", tunnel=" + tunnel +
                ", idBrokerMappingSource=" + idBrokerMappingSource +
                ", cloudStorageValidation=" + cloudStorageValidation +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", proxyConfigName='" + proxyConfigName + '\'' +
                ", aws=" + aws +
                ", azure=" + azure +
                ", tags=" + tags +
                ", parentEnvironmentName='" + parentEnvironmentName + '\'' +
                ", environmentServiceVersion='" + environmentServiceVersion + '\'' +
                '}';
    }

    public GcpEnvironmentParameters getGcp() {
        return gcp;
    }

    public void setGcp(GcpEnvironmentParameters gcp) {
        this.gcp = gcp;
    }
}
