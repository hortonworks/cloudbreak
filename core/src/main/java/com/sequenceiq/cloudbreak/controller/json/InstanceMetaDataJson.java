package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.InstanceMetaDataModelDescription;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceMetaData")
public class InstanceMetaDataJson implements JsonEntity {

    @ApiModelProperty(InstanceMetaDataModelDescription.PRIVATE_IP)
    private String privateIp;
    @ApiModelProperty(InstanceMetaDataModelDescription.PUBLIC_IP)
    private String publicIp;
    @ApiModelProperty(InstanceMetaDataModelDescription.INSTANCE_ID)
    private String instanceId;
    @ApiModelProperty(InstanceMetaDataModelDescription.VOLUME_COUNT)
    private Integer volumeCount;
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
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
