package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DefaultImageCatalogV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DefaultImageCatalogResponse {

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.ImageSettingsModelDescription.DEFAULT_IMAGE_CATALOG_NAME,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.ImageSettingsModelDescription.DEFAULT_IMAGE_CATALOG_URL,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "DefaultImageCatalogResponse{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
