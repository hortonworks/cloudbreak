package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ClusterResponse implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ClusterModelDescription.STATUS)
    private String status;

    @ApiModelProperty(ClusterModelDescription.HOURS)
    private int hoursUp;

    @ApiModelProperty(ClusterModelDescription.MINUTES)
    private int minutesUp;

    @ApiModelProperty(ClusterModelDescription.CLUSTER_NAME)
    private String cluster;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintResponse blueprint;

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(ClusterModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;

    @ApiModelProperty(StackModelDescription.AMBARI_URL)
    private String ambariServerUrl;

    @ApiModelProperty(StackModelDescription.USERNAME)
    private String userName;

    private boolean secure;

    private Set<HostGroupResponse> hostGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_IDS)
    private Set<Long> rdsConfigIds = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.RDSCONFIGS)
    private Set<RDSConfigResponse> rdsConfigs = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.SERVICE_ENDPOINT_MAP)
    private Map<String, String> serviceEndPoints = new HashMap<>();

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
    private AmbariStackDetailsJson ambariStackDetails;

    @ApiModelProperty(ClusterModelDescription.AMBARI_REPO_DETAILS)
    private AmbariRepoDetailsJson ambariRepoDetailsJson;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_DATABASE_DETAILS)
    private AmbariDatabaseDetailsJson ambariDatabaseDetails;

    @ApiModelProperty(ClusterModelDescription.CUSTOM_QUEUE)
    private String customQueue;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
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

    public Set<Long> getRdsConfigIds() {
        return rdsConfigIds;
    }

    public void setRdsConfigId(Set<Long> rdsConfigIds) {
        this.rdsConfigIds = rdsConfigIds;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Map<String, String> getServiceEndPoints() {
        return serviceEndPoints;
    }

    public void setServiceEndPoints(Map<String, String> serviceEndPoints) {
        this.serviceEndPoints = serviceEndPoints;
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

    public Set<RDSConfigResponse> getRdsConfigs() {
        return rdsConfigs;
    }

    public void setRdsConfigs(Set<RDSConfigResponse> rdsConfigs) {
        this.rdsConfigs = rdsConfigs;
    }

    public LdapConfigResponse getLdapConfig() {
        return ldapConfig;
    }

    public void setLdapConfig(LdapConfigResponse ldapConfig) {
        this.ldapConfig = ldapConfig;
    }

    public BlueprintResponse getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintResponse blueprint) {
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

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
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
}
