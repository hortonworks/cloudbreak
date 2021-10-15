package com.sequenceiq.distrox.api.v1.distrox.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXGenerateImageCatalogV1Response {

    @NotNull
    private CloudbreakImageCatalogV3 imageCatalog;

    public DistroXGenerateImageCatalogV1Response(CloudbreakImageCatalogV3 imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public CloudbreakImageCatalogV3 getImageCatalog() {
        return imageCatalog;
    }

    @Override
    public String toString() {
        return "DistroXGenerateImageCatalogV1Response{" +
                "imageCatalog=" + imageCatalog +
                '}';
    }
}
