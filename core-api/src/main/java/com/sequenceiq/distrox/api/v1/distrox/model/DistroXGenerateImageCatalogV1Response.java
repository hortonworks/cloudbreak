package com.sequenceiq.distrox.api.v1.distrox.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXGenerateImageCatalogV1Response {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
