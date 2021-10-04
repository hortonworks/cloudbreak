package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ChangeImageCatalogRequest {

    @Size(max = 255, min = 1, message = "The length of the image catalog has to be in range of 1 to 255")
    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.ImageSettingsModelDescription.IMAGE_CATALOG, required = true)
    private String imageCatalog;

    public String getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(String imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    @Override
    public String toString() {
        return "ChangeImageCatalogRequest{" +
                "imageCatalog='" + imageCatalog + '\'' +
                '}';
    }
}
