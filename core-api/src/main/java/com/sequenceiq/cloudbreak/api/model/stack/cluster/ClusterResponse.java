package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.CustomContainerResponse;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemResponse;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.annotations.TransformSetterType;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.annotations.ApiModelProperty;

public class ClusterResponse implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.HOURS)
    private int hoursUp;

    @ApiModelProperty(ClusterModelDescription.MINUTES)
    private int minutesUp;

    @TransformSetterType
    @ApiModelProperty(ClusterModelDescription.CLUSTER_NAME)
    private String cluster;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintV4Response blueprint;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ClusterModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;

    @ApiModelProperty(StackModelDescription.AMBARI_URL)
    private String ambariServerUrl;

    @ApiModelProperty(StackModelDescription.DP_AMBARI_USERNAME)
    private SecretResponse dpAmbariUser;

    @ApiModelProperty(StackModelDescription.DP_AMBARI_PASSWORD)
    private SecretResponse dpAmbariPassword;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupResponse> hostGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_NAMES)
    private Set<Long> rdsConfigIds = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.RDSCONFIGS)
    private Set<DatabaseV4Response> rdsConfigs = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.PROXY_NAME)
    private String proxyName;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_EXPOSED_SERVICES)
    private Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesForTopologies;

    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG_ID)
    private Long ldapConfigId;

    @ApiModelProperty(ClusterModelDescription.LDAP_CONFIG)
    private LdapConfigResponse ldapConfig;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_ATTRIBUTES)
    private Map<String, Object> attributes = new HashMap<>();

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_INPUTS)
    private Set<BlueprintInputJson> blueprintInputs = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_CUSTOM_PROPERTIES)
    private String blueprintCustomProperties;

    @ApiModelProperty(ClusterModelDescription.EXECUTOR_TYPE)
    private ExecutorType executorType;

    private GatewayJson gateway;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_CONTAINERS)
    private CustomContainerResponse customContainers;

    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsResponse ambariStackDetails;

    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepoDetailsJson ambariRepoDetailsJson;

    @ApiModelProperty(ClusterModelDescription.AMBARI_DATABASE_DETAILS)
    private @Valid AmbariDatabaseDetailsJson ambariDatabaseDetails;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

    @ApiModelProperty(ClusterModelDescription.CREATION_FINISHED)
    private Long creationFinished;

    private KerberosResponse kerberosResponse;

    @ApiModelProperty(ClusterModelDescription.UPTIME)
    private Long uptime;

    @ApiModelProperty(ModelDescriptions.BlueprintModelDescription.AMBARI_BLUEPRINT)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String extendedBlueprintText;

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE)
    private SharedServiceResponse sharedServiceResponse;

    @ApiModelProperty(ClusterModelDescription.FILESYSTEM)
    private FileSystemResponse fileSystemResponse;

    @ApiModelProperty(ModelDescriptions.WORKSPACE_OF_THE_RESOURCE)
    private WorkspaceResourceResponse workspace;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @JsonRawValue
    public String getCluster() {
        return cluster;
    }

    public void setCluster(JsonNode node) {
        cluster = node.toString();
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Set<HostGroupResponse> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupResponse> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    public String getAmbariServerUrl() {
        return ambariServerUrl;
    }

    public void setAmbariServerUrl(String ambariServerUrl) {
        this.ambariServerUrl = ambariServerUrl;
    }

    public SecretResponse getDpAmbariUser() {
        return dpAmbariUser;
    }

    public void setDpAmbariUser(SecretResponse dpAmbariUser) {
        this.dpAmbariUser = dpAmbariUser;
    }

    public SecretResponse getDpAmbariPassword() {
        return dpAmbariPassword;
    }

    public void setDpAmbariPassword(SecretResponse dpAmbariPassword) {
        this.dpAmbariPassword = dpAmbariPassword;
    }

    public Set<Long> getRdsConfigIds() {
        return rdsConfigIds;
    }

    public void setRdsConfigId(Set<Long> rdsConfigIds) {
        this.rdsConfigIds = rdsConfigIds;
    }

    public Map<String, Collection<ClusterExposedServiceResponse>> getClusterExposedServicesForTopologies() {
        return clusterExposedServicesForTopologies;
    }

    public void setClusterExposedServicesForTopologies(Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesForTopologies) {
        this.clusterExposedServicesForTopologies = clusterExposedServicesForTopologies;
    }

    public ConfigStrategy getConfigStrategy() {
        return configStrategy;
    }

    public void setConfigStrategy(ConfigStrategy configStrategy) {
        this.configStrategy = configStrategy;
    }

    public Long getLdapConfigId() {
        return ldapConfigId;
    }

    public void setLdapConfigId(Long ldapConfigId) {
        this.ldapConfigId = ldapConfigId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Set<BlueprintInputJson> getBlueprintInputs() {
        return blueprintInputs;
    }

    public void setBlueprintInputs(Set<BlueprintInputJson> blueprintInputs) {
        this.blueprintInputs = blueprintInputs;
    }

    @JsonRawValue
    public String getBlueprintCustomProperties() {
        return blueprintCustomProperties;
    }

    public void setBlueprintCustomProperties(JsonNode blueprintCustomProperties) {
        this.blueprintCustomProperties = blueprintCustomProperties.toString();
    }

    public Set<DatabaseV4Response> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<DatabaseV4Response> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public LdapConfigResponse getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigResponse ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public BlueprintV4Response getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4Response blueprint) {
        this.blueprint = blueprint;
    }

    public GatewayJson getGateway() {
        return gateway;
    }

    public void setGateway(GatewayJson gateway) {
        this.gateway = gateway;
    }

    public CustomContainerResponse getCustomContainers() {
        return customContainers;
    }

    public void setCustomContainers(CustomContainerResponse customContainers) {
        this.customContainers = customContainers;
    }

    public AmbariStackDetailsResponse getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsResponse ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
    }

    public AmbariRepoDetailsJson getAmbariRepoDetailsJson() {
        return ambariRepoDetailsJson;
    }

    public void setAmbariRepoDetailsJson(AmbariRepoDetailsJson ambariRepoDetailsJson) {
        this.ambariRepoDetailsJson = ambariRepoDetailsJson;
    }

    public AmbariDatabaseDetailsJson getAmbariDatabaseDetails() {
        return ambariDatabaseDetails;
    }

    public void setAmbariDatabaseDetails(AmbariDatabaseDetailsJson ambariDatabaseDetails) {
        this.ambariDatabaseDetails = ambariDatabaseDetails;
    }

    public String getCustomQueue() {
        return customQueue;
    }

    public void setCustomQueue(String customQueue) {
        this.customQueue = customQueue;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    public Long getCreationFinished() {
        return creationFinished;
    }

    public void setCreationFinished(Long creationFinished) {
        this.creationFinished = creationFinished;
    }

    public KerberosResponse getKerberosResponse() {
        return kerberosResponse;
    }

    public void setKerberosResponse(KerberosResponse kerberosResponse) {
        this.kerberosResponse = kerberosResponse;
    }

    public Long getUptime() {
        return uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public String getExtendedBlueprintText() {
        return extendedBlueprintText;
    }

    public void setExtendedBlueprintText(String extendedBlueprintText) {
        this.extendedBlueprintText = extendedBlueprintText;
    }

    public SharedServiceResponse getSharedServiceResponse() {
        return sharedServiceResponse;
    }

    public void setSharedServiceResponse(SharedServiceResponse sharedServiceResponse) {
        this.sharedServiceResponse = sharedServiceResponse;
    }

    public FileSystemResponse getFileSystemResponse() {
        return fileSystemResponse;
    }

    public void setFileSystemResponse(FileSystemResponse fileSystemResponse) {
        this.fileSystemResponse = fileSystemResponse;
    }

    public WorkspaceResourceResponse getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceResourceResponse workspace) {
        this.workspace = workspace;
    }
}
