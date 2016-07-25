package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackResponse extends StackBase {
    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long id;
    @ApiModelProperty(ModelDescriptions.OWNER)
    private String owner;
    @ApiModelProperty(ModelDescriptions.ACCOUNT)
    private String account;
    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;
    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;
    private ClusterResponse cluster;
    @ApiModelProperty(StackModelDescription.STATUS_REASON)
    private String statusReason;
    @Valid
    @ApiModelProperty
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    @ApiModelProperty(StackModelDescription.ORCHESTRATOR)
    private OrchestratorResponse orchestrator;
    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;
    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;
    @ApiModelProperty(StackModelDescription.IMAGE)
    private ImageJson image;
    @ApiModelProperty(StackModelDescription.CLOUDBREAK_DETAILS)
    private CloudbreakDetailsJson cloudbreakDetails;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public ClusterResponse getCluster() {
        return cluster;
    }

    public void setCluster(ClusterResponse cluster) {
        this.cluster = cluster;
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

    public OrchestratorResponse getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorResponse orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public ImageJson getImage() {
        return image;
    }

    public void setImage(ImageJson image) {
        this.image = image;
    }

    public CloudbreakDetailsJson getCloudbreakDetails() {
        return cloudbreakDetails;
    }

    public void setCloudbreakDetails(CloudbreakDetailsJson cloudbreakDetails) {
        this.cloudbreakDetails = cloudbreakDetails;
    }
}
