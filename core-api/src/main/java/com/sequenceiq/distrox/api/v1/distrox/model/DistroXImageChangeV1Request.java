package com.sequenceiq.distrox.api.v1.distrox.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistroXImageChangeV1Request implements JsonEntity {

    @Schema(description = StackModelDescription.IMAGE_ID, required = true)
    @NotNull
    @Size(min = 1, message = "The length of the imageId has to be greater than 1")
    private String imageId;

    @Schema(description = StackModelDescription.IMAGE_CATALOG)
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
}
