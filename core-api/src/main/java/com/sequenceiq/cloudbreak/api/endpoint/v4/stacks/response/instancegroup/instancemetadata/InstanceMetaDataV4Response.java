package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceMetaDataModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonInclude(Include.NON_NULL)
public class InstanceMetaDataV4Response implements JsonEntity {

    @Schema(description = InstanceMetaDataModelDescription.PRIVATE_IP)
    private String privateIp;

    @Schema(description = InstanceMetaDataModelDescription.PUBLIC_IP)
    private String publicIp;

    @Schema
    private Integer sshPort;

    @Schema(description = InstanceMetaDataModelDescription.INSTANCE_ID)
    private String instanceId;

    @Schema(description = ModelDescriptions.AMBARI_SERVER)
    private Boolean ambariServer;

    @Schema(description = InstanceMetaDataModelDescription.DISCOVERY_FQDN)
    private String discoveryFQDN;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @Schema(description = InstanceMetaDataModelDescription.SUBNET_ID)
    private String subnetId;

    @Schema(description = InstanceMetaDataModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @Schema(description = InstanceMetaDataModelDescription.RACK_ID)
    private String rackId;

    @Schema(description = InstanceGroupModelDescription.STATUS)
    private InstanceStatus instanceStatus;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_TYPE)
    private InstanceMetadataType instanceType;

    @Schema(description = HostMetadataModelDescription.STATE)
    private String state;

    @Schema(description = HostMetadataModelDescription.STATUS_REASON)
    private String statusReason;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MountedVolumeV4Response> mountedVolumes = Lists.newArrayList();

    @Schema
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
