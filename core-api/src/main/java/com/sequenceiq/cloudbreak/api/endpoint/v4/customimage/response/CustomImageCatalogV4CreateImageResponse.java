package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4CreateImageResponse implements JsonEntity {

    @JsonProperty
    private String imageId;

    @JsonProperty
    private String imageType;

    @JsonProperty
    private String sourceImageId;

    @JsonProperty
    private String baseParcelUrl;

    @JsonProperty
    private Set<CustomImageCatalogV4VmImageResponse> vmImages = new HashSet<>();

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
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

    public Set<CustomImageCatalogV4VmImageResponse> getVmImages() {
        return vmImages;
    }

    public void setVmImages(Set<CustomImageCatalogV4VmImageResponse> vmImages) {
        this.vmImages = vmImages;
    }
}
