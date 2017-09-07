package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateCluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterJson implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.HOSTGROUP_ADJUSTMENT)
    private HostGroupAdjustmentJson hostGroupAdjustment;

    @ApiModelProperty(ClusterModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @ApiModelProperty(ClusterModelDescription.USERNAME_PASSWORD)
    private UserNamePasswordJson userNamePasswordJson;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupRequest> hostgroups;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public HostGroupAdjustmentJson getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public void setHostGroupAdjustment(HostGroupAdjustmentJson hostGroupAdjustment) {
        this.hostGroupAdjustment = hostGroupAdjustment;
    }

    public Long getBlueprintId() {
        return blueprintId;
    }

    public Set<HostGroupRequest> getHostgroups() {
        return hostgroups;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public void setHostgroups(Set<HostGroupRequest> hostgroups) {
        this.hostgroups = hostgroups;
    }

    public boolean getValidateBlueprint() {
        return validateBlueprint == null ? false : validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
    }

    public UserNamePasswordJson getUserNamePasswordJson() {
        return userNamePasswordJson;
    }

    public void setUserNamePasswordJson(UserNamePasswordJson userNamePasswordJson) {
        this.userNamePasswordJson = userNamePasswordJson;
    }
}
