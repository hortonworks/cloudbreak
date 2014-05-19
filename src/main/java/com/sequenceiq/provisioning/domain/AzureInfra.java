package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class AzureInfra extends Infra implements ProvisionEntity {

    private String location;
    private String name;
    private String description;
    private String subnetAddressPrefix;
    private String deploymentSlot;
    private Boolean disableSshPasswordAuthentication;
    private String vmType;
    private String imageName;
    private String userName;
    private String password;

    @ManyToOne
    private User user;

    public AzureInfra() {
        super();
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

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getVmType() {
        return vmType;
    }

    public void setVmType(String vmType) {
        this.vmType = vmType;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
