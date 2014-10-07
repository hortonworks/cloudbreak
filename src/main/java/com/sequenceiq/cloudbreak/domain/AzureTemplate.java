package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    @Enumerated(EnumType.STRING)
    private AzureLocation location;

    private String vmType;
    private String imageName;

    public AzureTemplate() {
    }

    public AzureLocation getLocation() {
        return location;
    }

    public void setLocation(AzureLocation location) {
        this.location = location;
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

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Integer getMultiplier() {
        return 1;
    }

    public String nameAsFolder() {
        return getName().replaceAll("@", "_").replace(".", "_").replace(" ", "_");
    }
}
