package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ReinstallRequestV2")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReinstallRequestV2 implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.INSTANCE_GROUPS)
    private Set<InstanceGroupV2Request> instanceGroups;

    @Valid
    @ApiModelProperty(ClusterModelDescription.AMBARI_STACK_DETAILS)
    private AmbariStackDetailsJson ambariStackDetails;

    @NotNull
    @ApiModelProperty(ClusterModelDescription.BLUEPRINT_NAME)
    private String blueprintName;

    private String account;

    private Long stackId;

    public Set<InstanceGroupV2Request> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupV2Request> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public AmbariStackDetailsJson getAmbariStackDetails() {
        return ambariStackDetails;
    }

    public void setAmbariStackDetails(AmbariStackDetailsJson ambariStackDetails) {
        this.ambariStackDetails = ambariStackDetails;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    @JsonIgnore
    public String getAccount() {
        return account;
    }

    @JsonIgnore
    public void setAccount(String account) {
        this.account = account;
    }

    @JsonIgnore
    public Long getStackId() {
        return stackId;
    }

    @JsonIgnore
    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
