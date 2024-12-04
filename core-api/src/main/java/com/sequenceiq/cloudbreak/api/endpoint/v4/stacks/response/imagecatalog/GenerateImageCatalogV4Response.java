package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

import io.swagger.v3.oas.annotations.media.Schema;

public class GenerateImageCatalogV4Response {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private CloudbreakImageCatalogV3 imageCatalog;

    public GenerateImageCatalogV4Response(CloudbreakImageCatalogV3 imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public CloudbreakImageCatalogV3 getImageCatalog() {
        return imageCatalog;
    }
}
