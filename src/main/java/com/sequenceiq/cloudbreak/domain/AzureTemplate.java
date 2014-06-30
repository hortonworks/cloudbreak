package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    private String location;
    @Column(nullable = false)
    private String name;
    private String description;
    private String vmType;
    private String imageName;
    private String password;
    @OneToMany(mappedBy = "azureTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Port> ports = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "azureTemplate_azureTemplateOwner")
    private User azureTemplateOwner;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Port> getPorts() {
        return ports;
    }

    public void setPorts(Set<Port> ports) {
        this.ports = ports;
    }

    public User getAzureTemplateOwner() {
        return azureTemplateOwner;
    }

    public void setAzureTemplateOwner(User azureTemplateOwner) {
        this.azureTemplateOwner = azureTemplateOwner;
    }

    @Override
    public void setUser(User user) {
        this.azureTemplateOwner = user;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public User getOwner() {
        return azureTemplateOwner;
    }

    public String nameAsFolder() {
        return name.replaceAll("@", "_").replace(".", "_").replace(" ", "_");
    }
}
