package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class AzureStack implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User user;

    private Integer clusterSize;
    private String location;
    private String name;
    private String description;
    private String subnetAddressPrefix;
    private String deploymentSlot;
    private Boolean disableSshPasswordAuthentication;
    private String vmType;

    public AzureStack() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
