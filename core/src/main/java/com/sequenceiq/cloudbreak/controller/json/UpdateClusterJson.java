package com.sequenceiq.cloudbreak.controller.json;

import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateCluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterJson implements JsonEntity {

    @ApiModelProperty(required = true)
    private HostGroupAdjustmentJson hostGroupAdjustment;
    @ApiModelProperty(required = true)
    private StatusRequest status;
    @ApiModelProperty(StackModelDescription.BLUEPRINT_ID)
    private Long blueprintId;
    private Boolean validateBlueprint = true;
    private Set<HostGroupJson> hostgroups;
    @Valid
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
}
