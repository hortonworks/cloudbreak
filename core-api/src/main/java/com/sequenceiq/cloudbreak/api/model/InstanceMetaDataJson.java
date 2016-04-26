package com.sequenceiq.cloudbreak.api.model;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceMetaDataModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceMetaData")
public class InstanceMetaDataJson implements JsonEntity {

    @ApiModelProperty(InstanceMetaDataModelDescription.PRIVATE_IP)
    private String privateIp;
    @ApiModelProperty(InstanceMetaDataModelDescription.PUBLIC_IP)
    private String publicIp;
    @ApiModelProperty
    private Integer sshPort;
    @ApiModelProperty(InstanceMetaDataModelDescription.INSTANCE_ID)
    private String instanceId;
    @ApiModelProperty(InstanceMetaDataModelDescription.AMBARI_SERVER)
    private Boolean ambariServer;
    @ApiModelProperty(InstanceMetaDataModelDescription.DISCOVERY_FQDN)
    private String discoveryFQDN;
    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;
    @ApiModelProperty(InstanceGroupModelDescription.STATUS)
    private InstanceStatus instanceStatus;

    public InstanceMetaDataJson() {

    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Boolean getAmbariServer() {
        return ambariServer;
    }

    public void setAmbariServer(Boolean ambariServer) {
        this.ambariServer = ambariServer;
    }

    public String getDiscoveryFQDN() {
        return discoveryFQDN;
    }

    public void setDiscoveryFQDN(String discoveryFQDN) {
        this.discoveryFQDN = discoveryFQDN;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }
}
