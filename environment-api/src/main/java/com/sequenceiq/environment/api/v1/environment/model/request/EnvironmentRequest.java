package com.sequenceiq.environment.api.v1.environment.model.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentV1Request")
public class EnvironmentRequest extends EnvironmentBaseRequest implements CredentialAwareEnvRequest {

    static final String LENGHT_INVALID_MSG = "The length of the environments's name has to be in range of 5 to 28";

    @Size(max = 28, min = 5, message = LENGHT_INVALID_MSG)
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The environments's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @NotNull
    private String name;

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(value = EnvironmentModelDescription.LOCATION, required = true)
    @NotNull
    private LocationRequest location;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.NETWORK)
    private EnvironmentNetworkRequest network;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
    private TelemetryRequest telemetry;

    @Size(max = 100)
    @ApiModelProperty(EnvironmentModelDescription.CLOUD_PLATFORM)
    private String cloudPlatform;

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
    @ApiModelProperty(EnvironmentModelDescription.TAGS)
    private Map<String, String> tags = new HashMap<>();

    @ApiModelProperty(value = EnvironmentModelDescription.PARENT_ENVIRONMENT_NAME)
    private String parentEnvironmentName;

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

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions == null ? new HashSet<>() : regions;
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

    public EnvironmentNetworkRequest getNetwork() {
        return network;
    }

    public void setNetwork(EnvironmentNetworkRequest network) {
        this.network = network;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
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

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
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
}
