package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostMetadataModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceMetaDataModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareInfoV4Response implements JsonEntity {

    @NotNull
    @Schema(description = ModelDescriptions.NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = HostGroupModelDescription.HOST_GROUP_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String groupName;

    @Schema(description = HostMetadataModelDescription.STATE)
    private String state;

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

    @Schema(description = InstanceGroupModelDescription.STATUS)
    private InstanceStatus instanceStatus;

    @Schema(description = InstanceGroupModelDescription.INSTANCE_TYPE)
    private InstanceMetadataType instanceMetadataType;

    private String imageName;

    private String os;

    private String osType;

    private String imageCatalogUrl;

    private String imageId;

    private String imageCatalogName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> packageVersions = new HashMap<>();

    private String availabilityZone;

    private String subnetId;

    @Schema(description = InstanceGroupModelDescription.TEMPLATE)
    private InstanceTemplateV4Response template;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public InstanceMetadataType getInstanceMetadataType() {
        return instanceMetadataType;
    }

    public void setInstanceMetadataType(InstanceMetadataType instanceMetadataType) {
        this.instanceMetadataType = instanceMetadataType;
    }

    public InstanceTemplateV4Response getTemplate() {
        return template;
    }

    public void setTemplate(InstanceTemplateV4Response template) {
        this.template = template;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }

    public Map<String, String> getPackageVersions() {
        return packageVersions;
    }

    public void setPackageVersions(Map<String, String> packageVersions) {
        this.packageVersions = packageVersions;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HardwareInfoV4Response.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("groupName='" + groupName + "'")
                .add("state='" + state + "'")
                .add("privateIp='" + privateIp + "'")
                .add("publicIp='" + publicIp + "'")
                .add("sshPort=" + sshPort)
                .add("instanceId='" + instanceId + "'")
                .add("ambariServer=" + ambariServer)
                .add("discoveryFQDN='" + discoveryFQDN + "'")
                .add("instanceGroup='" + instanceGroup + "'")
                .add("instanceStatus=" + instanceStatus)
                .add("instanceMetadataType=" + instanceMetadataType)
                .add("imageName='" + imageName + "'")
                .add("os='" + os + "'")
                .add("osType='" + osType + "'")
                .add("imageCatalogUrl='" + imageCatalogUrl + "'")
                .add("imageId='" + imageId + "'")
                .add("imageCatalogName='" + imageCatalogName + "'")
                .add("packageVersions=" + packageVersions)
                .add("template=" + template)
                .toString();
    }

}
