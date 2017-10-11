package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("UpdateClusterV2")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateClusterJsonV2 implements JsonEntity {

    @ApiModelProperty(ClusterModelDescription.HOSTGROUP_ADJUSTMENT)
    private ClusterScaleRequestV2 scaleRequest;

    @ApiModelProperty(ClusterModelDescription.STATUS_REQUEST)
    private StatusRequest status;

    @ApiModelProperty(ClusterModelDescription.AMBARI_REQUEST)
    private UpdateAmbariRequest updateAmbariRequest;

    private String account;

    private Long stackId;

    public StatusRequest getStatus() {
        return status;
    }

    public void setStatus(StatusRequest status) {
        this.status = status;
    }

    public ClusterScaleRequestV2 getScaleRequest() {
        return scaleRequest;
    }

    public void setScaleRequest(ClusterScaleRequestV2 scaleRequest) {
        this.scaleRequest = scaleRequest;
    }

    public UpdateAmbariRequest getUpdateAmbariRequest() {
        return updateAmbariRequest;
    }

    public void setUpdateAmbariRequest(UpdateAmbariRequest updateAmbariRequest) {
        this.updateAmbariRequest = updateAmbariRequest;
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
