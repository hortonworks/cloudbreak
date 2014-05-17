package com.sequenceiq.provisioning.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureStackJson implements JsonEntity {

    @JsonProperty("clusterSize")
    private Integer clusterSize;
    @JsonProperty("location")
    private String location;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("subnetAddressPrefix")
    private String subnetAddressPrefix;
    @JsonProperty("deploymentSlot")
    private String deploymentSlot;
    @JsonProperty("disableSshPasswordAuthentication")
    private Boolean disableSshPasswordAuthentication;
    @JsonProperty("vmType")
    private String vmType;

    public AzureStackJson() {

    }

    public Integer getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(Integer clusterSize) {
        this.clusterSize = clusterSize;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubnetAddressPrefix() {
        return subnetAddressPrefix;
    }

    public void setSubnetAddressPrefix(String subnetAddressPrefix) {
        this.subnetAddressPrefix = subnetAddressPrefix;
    }

    public String getDeploymentSlot() {
        return deploymentSlot;
    }

    public void setDeploymentSlot(String deploymentSlot) {
        this.deploymentSlot = deploymentSlot;
    }

    public Boolean getDisableSshPasswordAuthentication() {
        return disableSshPasswordAuthentication;
    }

    public void setDisableSshPasswordAuthentication(Boolean disableSshPasswordAuthentication) {
        this.disableSshPasswordAuthentication = disableSshPasswordAuthentication;
    }

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
    }
}
