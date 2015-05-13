package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackResponse extends StackBase {
    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long id;
    @ApiModelProperty(StackModelDescription.OWNER)
    private String owner;
    @ApiModelProperty(StackModelDescription.ACCOUNT)
    private String account;
    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;
    @ApiModelProperty(StackModelDescription.STATUS)
    private Status status;
    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;
    private ClusterResponse cluster;
    @ApiModelProperty(StackModelDescription.STATUS_REASON)
    private String statusReason;
    @Valid
    @ApiModelProperty
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    @ApiModelProperty(StackModelDescription.CONSUL_SERVER_COUNT)
    private Integer consulServerCount;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    public ClusterResponse getCluster() {
        return cluster;
    }

    public void setCluster(ClusterResponse cluster) {
        this.cluster = cluster;
    }

    public Integer getConsulServerCount() {
        return consulServerCount;
    }

    public void setConsulServerCount(Integer consulServerCount) {
        this.consulServerCount = consulServerCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<InstanceGroupJson> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupJson> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
