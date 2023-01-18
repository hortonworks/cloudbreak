package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.BASE_PARCEL_URL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.IMAGE_TYPE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.SOURCE_IMAGE_ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.VM_IMAGES;

import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.customimage.UniqueRegion;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4UpdateImageRequest {

    @Schema(description = IMAGE_TYPE)
    private String imageType;

    @Size(max = 255, message = "The length of the sourceImageId must be less than 255")
    @Pattern(regexp = "(^[a-z0-9][-a-z0-9]*[a-z0-9]$)",
            message = "The sourceImageId can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @Schema(description = SOURCE_IMAGE_ID)
    private String sourceImageId;

    @Size(max = 255, message = "The length of the baseParcelUrl must be less than 255")
    @Schema(description = BASE_PARCEL_URL)
    private String baseParcelUrl;

    @Schema(description = VM_IMAGES)
    @UniqueRegion
    private Set<CustomImageCatalogV4VmImageRequest> vmImages;

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public String getSourceImageId() {
        return sourceImageId;
    }

    public void setSourceImageId(String sourceImageId) {
        this.sourceImageId = sourceImageId;
    }

    public String getBaseParcelUrl() {
        return baseParcelUrl;
    }

    public void setBaseParcelUrl(String baseParcelUrl) {
        this.baseParcelUrl = baseParcelUrl;
    }

    public Set<CustomImageCatalogV4VmImageRequest> getVmImages() {
        return vmImages;
    }

    public void setVmImages(Set<CustomImageCatalogV4VmImageRequest> vmImages) {
        this.vmImages = vmImages;
    }
}
