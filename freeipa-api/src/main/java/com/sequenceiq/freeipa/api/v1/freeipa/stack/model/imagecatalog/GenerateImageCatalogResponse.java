package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;

import io.swagger.annotations.ApiModel;

@ApiModel("GenerateImageCatalogV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateImageCatalogResponse {

    @NotNull
    private ImageCatalog imageCatalog;

    public ImageCatalog getImageCatalog() {
        return imageCatalog;
    }

    public void setImageCatalog(ImageCatalog imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    @Override
    public String toString() {
        return "GenerateImageCatalogResponse{" +
                "imageCatalog=" + imageCatalog +
                '}';
    }
}
