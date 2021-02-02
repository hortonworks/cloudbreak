package com.sequenceiq.cloudbreak.cloud.azure.image;

public class AzureImageInfo {

    private final String imageNameWithRegion;

    private final String imageName;

    private final String imageId;

    private final String region;

    private final String resourceGroup;

    public AzureImageInfo(String imageNameWithRegion, String imageName, String imageId, String region, String resourceGroup) {
        this.imageNameWithRegion = imageNameWithRegion;
        this.imageId = imageId;
        this.region = region;
        this.resourceGroup = resourceGroup;
        this.imageName = imageName;
    }

    public String getImageNameWithRegion() {
        return imageNameWithRegion;
    }

    public String getImageName() {
        return imageName;
    }

    public String getImageId() {
        return imageId;
    }

    public String getRegion() {
        return region;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    @Override
    public String toString() {
        return "AzureImageInfo{" +
                "imageNameWithRegion='" + imageNameWithRegion + '\'' +
                ", imageName='" + imageName + '\'' +
                ", imageId='" + imageId + '\'' +
                ", region='" + region + '\'' +
                ", resourceGroup='" + resourceGroup + '\'' +
                '}';
    }
}