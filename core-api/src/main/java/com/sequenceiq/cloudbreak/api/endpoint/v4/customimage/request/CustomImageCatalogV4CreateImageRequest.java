package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.validation.customimage.UniqueRegion;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.BASE_PARCEL_URL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.IMAGE_TYPE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.SOURCE_IMAGE_ID;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.CustomImageDescription.VM_IMAGES;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4CreateImageRequest {

    @NotNull
    @ApiModelProperty(value = IMAGE_TYPE, required = true)
    private String imageType;

    @Size(max = 255, min = 1, message = "The length of the sourceImageId must be between 1 and 255")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The sourceImageId can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = SOURCE_IMAGE_ID, required = true)
    private String sourceImageId;

    @Size(max = 255, min = 1, message = "The length of the baseParcelUrl must be between 1 and 255")
    @NotNull
    @ApiModelProperty(value = BASE_PARCEL_URL, required = true)
    private String baseParcelUrl;

    @NotNull
    @ApiModelProperty(value = VM_IMAGES, required = true)
    @UniqueRegion
    private Set<CustomImageCatalogV4VmImageRequest> vmImages = new HashSet<>();

    public CustomImageCatalogV4CreateImageRequest() {
    }

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
