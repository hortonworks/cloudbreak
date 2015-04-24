package com.sequenceiq.cloudbreak.controller.json;

import java.util.Set;

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
    private Set<HostGroupJson> hostgroups;

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
}
