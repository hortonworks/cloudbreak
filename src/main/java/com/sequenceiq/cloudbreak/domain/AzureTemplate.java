package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private AzureLocation location;
    @Column(nullable = false)
    private String name;
    private String description;
    private String vmType;
    private String imageName;

    @ManyToOne
    @JoinColumn(name = "azureTemplate_azureTemplateOwner")
    private User azureTemplateOwner;

    public AzureTemplate() {
    }

    public AzureLocation getLocation() {
        return location;
    }

    public void setLocation(AzureLocation location) {
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

    @Override
    public Integer getMultiplier() {
        return 1;
    }

    public String nameAsFolder() {
        return name.replaceAll("@", "_").replace(".", "_").replace(" ", "_");
    }
}
