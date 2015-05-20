package com.sequenceiq.cloudbreak.controller.json;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.ClusterModelDescription;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class ClusterResponse {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;
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
    private Set<HostGroupJson> hostGroups;
    private AmbariStackDetailsJson ambariStackDetails;

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
}
