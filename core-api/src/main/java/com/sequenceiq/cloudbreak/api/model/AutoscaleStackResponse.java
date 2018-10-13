package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoscaleStackResponse {

    @ApiModelProperty(StackModelDescription.STACK_ID)
    private Long stackId;

    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.GATEWAY_PORT)
    private Integer gatewayPort;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;

    @ApiModelProperty(StackModelDescription.USERNAME)
    private String userName;

    @ApiModelProperty(StackModelDescription.PASSWORD)
    private String password;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status clusterStatus;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public void setGatewayPort(Integer gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Status clusterStatus) {
        this.clusterStatus = clusterStatus;
    }
}
