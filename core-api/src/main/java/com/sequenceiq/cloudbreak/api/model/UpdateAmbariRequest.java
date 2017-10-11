package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateAmbariRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateAmbariRequest implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.USERNAME_PASSWORD)
    private UserNamePasswordJson userNamePasswordJson;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_ID)
    private String blueprintName;

    @ApiModelProperty(ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupRequest> hostgroups;

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
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

    public Set<HostGroupRequest> getHostgroups() {
        return hostgroups;
    }

    public void setHostgroups(Set<HostGroupRequest> hostgroups) {
        this.hostgroups = hostgroups;
    }
}
