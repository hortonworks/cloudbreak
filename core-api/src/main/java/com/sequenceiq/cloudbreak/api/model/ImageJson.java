package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ImageModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageJson implements JsonEntity {
    @ApiModelProperty(ImageModelDescription.IMAGE_NAME)
    private String imageName;

    @ApiModelProperty(ImageModelDescription.IMAGE_CATALOG_URL)
    private String imageCatalogUrl;

    @ApiModelProperty(ImageModelDescription.IMAGE_ID)
    private String imageId;

    @ApiModelProperty(ImageModelDescription.IMAGE_CATALOG_NAME)
    private String imageCatalogName;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public void setImageCatalogUrl(String imageCatalogUrl) {
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }
}
