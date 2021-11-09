package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

public class GenerateImageCatalogV4Response {

    @NotNull
    private CloudbreakImageCatalogV3 imageCatalog;

    public GenerateImageCatalogV4Response(CloudbreakImageCatalogV3 imageCatalog) {
        this.imageCatalog = imageCatalog;
    }

    public CloudbreakImageCatalogV3 getImageCatalog() {
        return imageCatalog;
    }
}
