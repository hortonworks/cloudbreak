package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterV4Response implements JsonEntity {

    @Schema(description = ModelDescriptions.ID)
    private Long id;

    @Schema(description = ModelDescriptions.NAME)
    private String name;

    @Schema(description = ClusterModelDescription.STATUS)
    private Status status;

    @Schema(description = ClusterModelDescription.HOURS, requiredMode = Schema.RequiredMode.REQUIRED)
    private int hoursUp;

    @Schema(description = ClusterModelDescription.MINUTES, requiredMode = Schema.RequiredMode.REQUIRED)
    private int minutesUp;

    @Schema(description = ModelDescriptions.DESCRIPTION)
    private String description;

    @Schema(description = ClusterModelDescription.STATUS_REASON)
    private String statusReason;

    @Schema(description = ClusterModelDescription.DATABASES, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DatabaseV4Response> databases = new ArrayList<>();

    @Schema(description = ClusterModelDescription.PROXY_CRN)
    private String proxyConfigCrn;

    private String proxyConfigName;

    @Schema(description = ClusterModelDescription.FILESYSTEM)
    private CloudStorageResponse cloudStorage;

    private ClouderaManagerV4Response cm;

    private GatewayV4Response gateway;

    @Schema(description = ClusterModelDescription.CLUSTER_ATTRIBUTES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> attributes = new HashMap<>();

    @Schema(description = ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerV4Response customContainers;

    @Schema(description = ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    @Schema(description = ClusterModelDescription.CREATION_FINISHED)
    private Long creationFinished;

    @Schema(description = ClusterModelDescription.UPTIME)
    private Long uptime;

    @Schema(description = ClusterModelDescription.CLUSTER_EXPOSED_SERVICES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Collection<ClusterExposedServiceV4Response>> exposedServices = new HashMap<>();

    @Schema(description = ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceV4Response workspace;

    @Schema(description = StackModelDescription.CM_MANAGEMENT_USERNAME)
    private SecretResponse cmMgmtUser;

    @Schema(description = StackModelDescription.CM_MANAGEMENT_PASSWORD)
    private SecretResponse cmMgmtPassword;

    @Schema(description = ClusterModelDescription.BLUEPRINT)
    private BlueprintV4Response blueprint;

    @Schema(description = BlueprintModelDescription.BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String extendedBlueprintText;

    @Schema(description = StackModelDescription.CUSTOM_CONFIGURATIONS_NAME)
    private String customConfigurationsName;

    @Schema(description = StackModelDescription.CUSTOM_CONFIGURATIONS_CRN)
    private String customConfigurationsCrn;

    @Schema(description = StackModelDescription.SERVER_IP)
    private String serverIp;

    @Schema(description = StackModelDescription.SERVER_FQDN)
    private String serverFqdn;

    @Schema(description = StackModelDescription.SERVER_URL)
    private String serverUrl;

    @Schema(description = ClusterModelDescription.REDBEAMS_DB_SERVER_CRN)
    private String databaseServerCrn;

    @Schema(description = ClusterModelDescription.ENABLE_RANGER_RAZ, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRazEnabled;

    @Schema(description = ClusterModelDescription.ENABLE_RANGER_RMS, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean rangerRmsEnabled;

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    @Schema(description = ClusterModelDescription.DATABASE_SSL_ENABLED, requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean dbSSLEnabled;

    private String dbSslRootCertBundle;

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION_DETAILS)
    private String certExpirationDetails;

    @Schema(description = ClusterModelDescription.ENCRYPTION_PROFILE_NAME)
    private String encryptionProfileName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getHoursUp() {
        return hoursUp;
    }

    public void setHoursUp(int hoursUp) {
        this.hoursUp = hoursUp;
    }

    public int getMinutesUp() {
        return minutesUp;
    }

    public void setMinutesUp(int minutesUp) {
        this.minutesUp = minutesUp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public CloudStorageResponse getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(CloudStorageResponse cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public ClouderaManagerV4Response getCm() {
        return cm;
    }

    public void setCm(ClouderaManagerV4Response cm) {
        this.cm = cm;
    }

    public GatewayV4Response getGateway() {
        return gateway;
    }

    public void setGateway(GatewayV4Response gateway) {
        this.gateway = gateway;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public Long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public Long getUptime() {
        return uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    public SecretResponse getCmMgmtUser() {
        return cmMgmtUser;
    }

    public void setCmMgmtUser(SecretResponse cmMgmtUser) {
        this.cmMgmtUser = cmMgmtUser;
    }

    public SecretResponse getCmMgmtPassword() {
        return cmMgmtPassword;
    }

    public void setCmMgmtPassword(SecretResponse cmMgmtPassword) {
        this.cmMgmtPassword = cmMgmtPassword;
    }

    public WorkspaceResourceV4Response getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceV4Response workspace) {
        this.workspace = workspace;
    }

    public List<DatabaseV4Response> getDatabases() {
        return databases;
    }

    public void setDatabases(List<DatabaseV4Response> databases) {
        this.databases = databases;
    }

    public String getProxyConfigCrn() {
        return proxyConfigCrn;
    }

    public void setProxyConfigCrn(String proxyConfigCrn) {
        this.proxyConfigCrn = proxyConfigCrn;
    }

    public CustomContainerV4Response getCustomContainers() {
        return customContainers;
    }

    public void setCustomContainers(CustomContainerV4Response customContainers) {
        this.customContainers = customContainers;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Map<String, Collection<ClusterExposedServiceV4Response>> exposedServices) {
        this.exposedServices = exposedServices;
    }

    public BlueprintV4Response getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4Response blueprint) {
        this.blueprint = blueprint;
    }

    public String getExtendedBlueprintText() {
        return extendedBlueprintText;
    }

    public void setExtendedBlueprintText(String extendedBlueprintText) {
        this.extendedBlueprintText = extendedBlueprintText;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getProxyConfigName() {
        return proxyConfigName;
    }

    public void setProxyConfigName(String proxyConfigName) {
        this.proxyConfigName = proxyConfigName;
    }

    public String getServerFqdn() {
        return serverFqdn;
    }

    public void setServerFqdn(String serverFqdn) {
        this.serverFqdn = serverFqdn;
    }

    public String getDatabaseServerCrn() {
        return databaseServerCrn;
    }

    public void setDatabaseServerCrn(String databaseServerCrn) {
        this.databaseServerCrn = databaseServerCrn;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean isRangerRmsEnabled() {
        return rangerRmsEnabled;
    }

    public void setRangerRmsEnabled(boolean rangerRmsEnabled) {
        this.rangerRmsEnabled = rangerRmsEnabled;
    }

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    public String getCustomConfigurationsCrn() {
        return customConfigurationsCrn;
    }

    public void setCustomConfigurationsCrn(String customConfigurationsCrn) {
        this.customConfigurationsCrn = customConfigurationsCrn;
    }

    public String getCustomConfigurationsName() {
        return customConfigurationsName;
    }

    public void setCustomConfigurationsName(String customConfigurationsName) {
        this.customConfigurationsName = customConfigurationsName;
    }

    public boolean isDbSSLEnabled() {
        return dbSSLEnabled;
    }

    public void setDbSSLEnabled(boolean dbSSLEnabled) {
        this.dbSSLEnabled = dbSSLEnabled;
    }

    public String getDbSslRootCertBundle() {
        return dbSslRootCertBundle;
    }

    public void setDbSslRootCertBundle(String dbSslRootCertBundle) {
        this.dbSslRootCertBundle = dbSslRootCertBundle;
    }

    public String getCertExpirationDetails() {
        return certExpirationDetails;
    }

    public void setCertExpirationDetails(String certExpirationDetails) {
        this.certExpirationDetails = certExpirationDetails;
    }

    public String getEncryptionProfileName() {
        return encryptionProfileName;
    }

    public void setEncryptionProfileName(String encryptionProfileName) {
        this.encryptionProfileName = encryptionProfileName;
    }
}
