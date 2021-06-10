package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceMetaDataModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonInclude(Include.NON_NULL)
public class InstanceMetaDataV4Response implements JsonEntity {

    @ApiModelProperty(InstanceMetaDataModelDescription.PRIVATE_IP)
    private String privateIp;

    @ApiModelProperty(InstanceMetaDataModelDescription.PUBLIC_IP)
    private String publicIp;

    @ApiModelProperty
    private Integer sshPort;

    @ApiModelProperty(InstanceMetaDataModelDescription.INSTANCE_ID)
    private String instanceId;

    @ApiModelProperty(ModelDescriptions.AMBARI_SERVER)
    private Boolean ambariServer;

    @ApiModelProperty(InstanceMetaDataModelDescription.DISCOVERY_FQDN)
    private String discoveryFQDN;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @ApiModelProperty(InstanceMetaDataModelDescription.SUBNET_ID)
    private String subnetId;

    @ApiModelProperty(InstanceMetaDataModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @ApiModelProperty(InstanceMetaDataModelDescription.RACK_ID)
    private String rackId;

    @ApiModelProperty(InstanceGroupModelDescription.STATUS)
    private InstanceStatus instanceStatus;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_TYPE)
    private InstanceMetadataType instanceType;

    @ApiModelProperty(HostMetadataModelDescription.STATE)
    private String state;

    @ApiModelProperty(HostMetadataModelDescription.STATUS_REASON)
    private String statusReason;

    @ApiModelProperty
    private List<MountedVolumeV4Response> mountedVolumes = Lists.newArrayList();

    @ApiModelProperty
    private InstanceLifeCycle lifeCycle;

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

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getRackId() {
        return rackId;
    }

    public void setRackId(String rackId) {
        this.rackId = rackId;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public void setInstanceType(InstanceMetadataType instanceType) {
        this.instanceType = instanceType;
    }

    public InstanceMetadataType getInstanceType() {
        return instanceType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public List<MountedVolumeV4Response> getMountedVolumes() {
        return mountedVolumes;
    }

    public void setMountedVolumes(List<MountedVolumeV4Response> mountedVolumes) {
        this.mountedVolumes = mountedVolumes;
    }

    public InstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(InstanceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }
}
