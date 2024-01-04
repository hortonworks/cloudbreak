package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.VmImageDescription.IMAGE_REFERENCE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.VmImageDescription.REGION;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4VmImageRequest implements JsonEntity {

    @Size(max = 255, min = 1, message = "The length of the region must be between 1 and 255")
    @NotNull
    @Schema(description = REGION, required = true)
    private String region;

    @Size(max = 255, min = 1, message = "The length of the imageReference must be between 1 and 255")
    @NotNull
    @Schema(description = IMAGE_REFERENCE, required = true)
    private String imageReference;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getImageReference() {
        return imageReference;
    }

    public void setImageReference(String imageReference) {
        this.imageReference = imageReference;
    }
}
