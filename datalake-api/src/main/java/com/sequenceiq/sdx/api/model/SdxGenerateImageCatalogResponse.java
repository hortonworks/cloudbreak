package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxGenerateImageCatalogResponse {

    @NotNull
    @Schema(description = ModelDescriptions.IMAGE_CATALOG)
    private CloudbreakImageCatalogV3 imageCatalog;

    public SdxGenerateImageCatalogResponse(CloudbreakImageCatalogV3 imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public CloudbreakImageCatalogV3 getImageCatalog() {
        return imageCatalog;
    }

    @Override
    public String toString() {
        return "SdxGenerateImageCatalogResponse{" + "imageCatalog=" + imageCatalog + '}';
    }
}
