package com.sequenceiq.cloudbreak.cloud.azure.image;

public class AzureImageCopyDetails {

    private final String imageStorageName;

    private final String imageResourceGroupName;

    private final String imageSource;

    AzureImageCopyDetails(String imageStorageName, String imageResourceGroupName, String imageSource) {
        this.imageStorageName = imageStorageName;
        this.imageResourceGroupName = imageResourceGroupName;
        this.imageSource = imageSource;
    }

    public String getImageStorageName() {
        return imageStorageName;
    }

    public String getImageResourceGroupName() {
        return imageResourceGroupName;
    }

    public String getImageSource() {
        return imageSource;
    }
}
