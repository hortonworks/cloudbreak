package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.common.model.annotations.TransformGetterType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterV4Request implements JsonEntity {

    @Schema(description = ClusterModelDescription.HOSTGROUP_ADJUSTMENT)
    private HostGroupAdjustmentV4Request hostGroupAdjustment;

    @Schema(description = ClusterModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @Schema(description = ClusterModelDescription.USERNAME_PASSWORD)
    private UserNamePasswordV4Request userNamePassword;

    @Schema(description = ClusterModelDescription.BLUEPRINT_ID)
    private String blueprintName;

    @TransformGetterType
    @Schema(description = ClusterModelDescription.VALIDATE_BLUEPRINT)
    private Boolean validateBlueprint = Boolean.TRUE;

    @Schema(description = ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupV4Request> hostgroups;

    public HostGroupAdjustmentV4Request getHostGroupAdjustment() {
        return hostGroupAdjustment;
    }

    public void setHostGroupAdjustment(HostGroupAdjustmentV4Request hostGroupAdjustment) {
        this.hostGroupAdjustment = hostGroupAdjustment;
    }

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public UserNamePasswordV4Request getUserNamePassword() {
        return userNamePassword;
    }

    public void setUserNamePassword(UserNamePasswordV4Request userNamePassword) {
        this.userNamePassword = userNamePassword;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public Boolean getValidateBlueprint() {
        return validateBlueprint;
    }

    public void setValidateBlueprint(Boolean validateBlueprint) {
        this.validateBlueprint = validateBlueprint;
    }

    public Set<HostGroupV4Request> getHostgroups() {
        return hostgroups;
    }

    public void setHostgroups(Set<HostGroupV4Request> hostgroups) {
        this.hostgroups = hostgroups;
    }

    @Override
    public String toString() {
        return "UpdateClusterV4Request{" +
                "hostGroupAdjustment=" + hostGroupAdjustment +
                ", status=" + status +
                ", userNamePassword=" + userNamePassword +
                ", blueprintName='" + blueprintName + '\'' +
                ", validateBlueprint=" + validateBlueprint +
                ", hostgroups=" + hostgroups +
                '}';
    }
}
