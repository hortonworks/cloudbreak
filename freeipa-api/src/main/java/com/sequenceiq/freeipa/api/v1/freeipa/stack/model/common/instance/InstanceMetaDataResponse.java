package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.HostMetadataModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.InstanceMetaDataModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("InstanceMetaDataV1Response")
@JsonInclude(Include.NON_NULL)
public class InstanceMetaDataResponse {

    @ApiModelProperty(InstanceMetaDataModelDescription.PRIVATE_IP)
    private String privateIp;

    @ApiModelProperty(InstanceMetaDataModelDescription.PUBLIC_IP)
    private String publicIp;

    @ApiModelProperty
    private Integer sshPort;

    @ApiModelProperty(InstanceMetaDataModelDescription.INSTANCE_ID)
    private String instanceId;

    @ApiModelProperty(InstanceMetaDataModelDescription.DISCOVERY_FQDN)
    private String discoveryFQDN;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_GROUP_NAME)
    private String instanceGroup;

    @ApiModelProperty(InstanceGroupModelDescription.STATUS)
    private InstanceStatus instanceStatus;

    @ApiModelProperty(InstanceGroupModelDescription.INSTANCE_TYPE)
    private InstanceMetadataType instanceType;

    @ApiModelProperty(HostMetadataModelDescription.STATE)
    private String state;

    @ApiModelProperty
    private InstanceLifeCycle lifeCycle;

    @ApiModelProperty(InstanceGroupModelDescription.SUBNET_ID)
    private String subnetId;

    @ApiModelProperty(InstanceGroupModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

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

    public InstanceLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(InstanceLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
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

    @Override
    public String toString() {
        return "InstanceMetaDataResponse{" +
                "privateIp='" + privateIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", sshPort=" + sshPort +
                ", instanceId='" + instanceId + '\'' +
                ", discoveryFQDN='" + discoveryFQDN + '\'' +
                ", instanceGroup='" + instanceGroup + '\'' +
                ", instanceStatus=" + instanceStatus +
                ", instanceType=" + instanceType +
                ", state='" + state + '\'' +
                ", lifeCycle=" + lifeCycle +
                ", subnetId='" + subnetId + '\'' +
                ", availabilityZone='" + availabilityZone + '\'' +
                '}';
    }
}
