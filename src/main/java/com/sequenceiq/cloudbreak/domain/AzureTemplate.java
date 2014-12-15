package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;

@Entity
public class AzureTemplate extends Template implements ProvisionEntity {

    private String vmType;
    private String imageName;

    public AzureTemplate() {
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

}
