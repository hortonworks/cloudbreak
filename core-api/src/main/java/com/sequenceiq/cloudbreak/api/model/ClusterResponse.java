package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ClusterResponse {

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
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;
    @ApiModelProperty(ClusterModelDescription.STATUS_REASON)
    private String statusReason;
    @ApiModelProperty(ModelDescriptions.StackModelDescription.AMBARI_IP)
    private String ambariServerIp;
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.USERNAME, required = true)
    private String userName;
    @ApiModelProperty(value = ModelDescriptions.StackModelDescription.PASSWORD, required = true)
    private String password;
    private boolean secure;
    @ApiModelProperty(value = ClusterModelDescription.LDAP_REQUIRED)
    private Boolean ldapRequired = false;
    @ApiModelProperty(value = ClusterModelDescription.SSSDCONFIG_ID)
    private Long sssdConfigId;
    private Set<HostGroupJson> hostGroups;
    private AmbariStackDetailsJson ambariStackDetails;
    @ApiModelProperty(ClusterModelDescription.RDSCONFIG_ID)
    private Long rdsConfigId;
    @ApiModelProperty(ClusterModelDescription.SERVICE_ENDPOINT_MAP)
    private Map<String, String> serviceEndPoints = new HashMap<>();
    @ApiModelProperty(ClusterModelDescription.CONFIG_STRATEGY)
    private ConfigStrategy configStrategy;
    @ApiModelProperty(ClusterModelDescription.ENABLE_SHIPYARD)
    private Boolean enableShipyard;
    @ApiModelProperty(value = ClusterModelDescription.LDAP_CONFIG_ID)
    private Long ldapConfigId;
    @ApiModelProperty(ClusterModelDescription.CLUSTER_ATTRIBUTES)
    private Map<String, Object> attributes;
    @ApiModelProperty(value = ClusterModelDescription.BLUEPRINT_INPUTS)
    private Set<BlueprintInputJson> blueprintInputs = new HashSet<>();

    public Boolean getEnableShipyard() {
        return enableShipyard;
    }

    public void setEnableShipyard(Boolean enableShipyard) {
        this.enableShipyard = enableShipyard;
    }

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
        this.cluster = node.toString();
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public Set<HostGroupJson> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupJson> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Boolean getLdapRequired() {
        return ldapRequired;
    }

    public void setLdapRequired(Boolean ldapRequired) {
        this.ldapRequired = ldapRequired;
    }

    public Long getSssdConfigId() {
        return sssdConfigId;
    }

    public void setSssdConfigId(Long sssdConfigId) {
        this.sssdConfigId = sssdConfigId;
    }

    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
    }

    public Long getRdsConfigId() {
        return rdsConfigId;
    }

    public void setRdsConfigId(Long rdsConfigId) {
        this.rdsConfigId = rdsConfigId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
