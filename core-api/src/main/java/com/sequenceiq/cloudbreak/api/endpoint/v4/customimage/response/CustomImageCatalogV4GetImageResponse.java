package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4GetImageResponse implements JsonEntity {

    @JsonProperty
    private String imageId;

    @JsonProperty
    private Long imageDate;

    @JsonProperty
    private String imageType;

    @JsonProperty
    private String sourceImageId;

    @JsonProperty
    private Long sourceImageDate;

    @JsonProperty
    private Map<String, String> versions;

    @JsonProperty
    private String cloudProvider;

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

    public Long getImageDate() {
        return imageDate;
    }

    public void setImageDate(Long imageDate) {
        this.imageDate = imageDate;
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

    public Long getSourceImageDate() {
        return sourceImageDate;
    }

    public void setSourceImageDate(Long sourceImageDate) {
        this.sourceImageDate = sourceImageDate;
    }

    public Map<String, String> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, String> versions) {
        this.versions = versions;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
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
