package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageCatalogDescription.DESCRIPTION;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageCatalogDescription.NAME;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4CreateRequest {

    @Size(max = 255, min = 1, message = "The length of the ID must be between 1 and 255")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @Schema(description = NAME, required = true)
    private String name;

    @Size(max = 255, message = "The length of the description must be less than 255")
    @Schema(description = DESCRIPTION)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
