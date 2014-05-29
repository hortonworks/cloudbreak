package com.sequenceiq.provisioning.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    private String location;
    private String name;
    private String description;
    private String subnetAddressPrefix;
    private String addressPrefix;
    private String deploymentSlot;
    private String vmType;
    private String imageName;
    private String userName;
    private String password;
    private String sshPublicKeyFingerprint;
    private String sshPublicKeyPath;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "azureTemplate_ports")
    private Set<Port> ports = new HashSet<>();

    @ManyToOne
    private User user;

    public AzureTemplate() {
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

    public String getSshPublicKeyFingerprint() {
        return sshPublicKeyFingerprint;
    }

    public void setSshPublicKeyFingerprint(String sshPublicKeyFingerprint) {
        this.sshPublicKeyFingerprint = sshPublicKeyFingerprint;
    }

    public String getSshPublicKeyPath() {
        return sshPublicKeyPath;
    }

    public void setSshPublicKeyPath(String sshPublicKeyPath) {
        this.sshPublicKeyPath = sshPublicKeyPath;
    }

    public String getAddressPrefix() {
        return addressPrefix;
    }

    public void setAddressPrefix(String addressPrefix) {
        this.addressPrefix = addressPrefix;
    }

    public Set<Port> getPorts() {
        return ports;
    }

    public void setPorts(Set<Port> ports) {
        this.ports = ports;
    }

    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
