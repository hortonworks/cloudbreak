package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class StackImageChangeV4Request implements JsonEntity {

    @ApiModelProperty(value = StackModelDescription.IMAGE_ID, required = true)
    @NotNull
    @Size(min = 1, message = "The length of the imageId has to be greater than 1")
    private String imageId;

    @ApiModelProperty(StackModelDescription.IMAGE_CATALOG)
    private String imageCatalogName;

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

    @Override
    public String toString() {
        return "StackImageChangeV4Request{" +
                "imageId='" + imageId + '\'' +
                ", imageCatalogName='" + imageCatalogName + '\'' +
                '}';
    }
}
