package com.sequenceiq.cloudbreak.service.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class StatedImage {

    private final Image image;

    private final String imageCatalogUrl;

    private final String imageCatalogName;

    @JsonCreator
    private StatedImage(
            @JsonProperty("image") Image image,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("imageCatalogName") String imageCatalogName) {
        this.image = image;
        this.imageCatalogUrl = imageCatalogUrl;
        this.imageCatalogName = imageCatalogName;
    }

    public Image getImage() {
        return image;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public static StatedImage statedImage(Image image, String imageCatalogUrl, String imageCatalogName) {
        return new StatedImage(image, imageCatalogUrl, imageCatalogName);
    }

    @Override
    public String toString() {
        return "StatedImage{"
                + "image=" + image
                + ", imageCatalogUrl='" + imageCatalogUrl + '\''
                + ", imageCatalogName='" + imageCatalogName + '\''
                + '}';
    }
}
