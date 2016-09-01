package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateCluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterJson implements JsonEntity {

    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.HOSTGROUP_ADJUSTMENT)
    private HostGroupAdjustmentJson hostGroupAdjustment;
    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.STATUS_REQUEST)
    private StatusRequest status;
    @ApiModelProperty(value = ModelDescriptions.ClusterModelDescription.USERNAME_PASSWORD)
    private UserNamePasswordJson userNamePasswordJson;
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.BLUEPRINT_ID)
    private Long blueprintId;
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupJson> hostgroups;
    @Valid
    @ApiModelProperty(ModelDescriptions.ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    public UpdateClusterJson() {
    }

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

    public Set<HostGroupJson> getHostgroups() {
        return hostgroups;
    }

    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }

    public void setHostgroups(Set<HostGroupJson> hostgroups) {
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
