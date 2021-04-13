package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageEntryV4Request {

    @Size(max = 48, min = 32, message = "The length of the ID must be between 32 and 48")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The ID can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.IMAGE_ID, required = true)
    private String id;

    @Size(max = 48, min = 32, message = "The length of the ID must be between 32 and 48")
    @Pattern(regexp = "(^[a-z][-a-z0-9]*[a-z0-9]$)",
            message = "The ID can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.SOURCE_IMAGE_ID, required = true)
    private String sourceId;

    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.PARCEL_BASE_URL)
    private String parcelBaseUrl;

    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.IMAGE_TYPE)
    private String imageType;

    @ApiModelProperty(value = ModelDescriptions.ImageModelDescription.VMS_TO_REGIONS)
    private Map<String, String> vmsToRegions = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getParcelBaseUrl() {
        return parcelBaseUrl;
    }

    public void setParcelBaseUrl(String parcelBaseUrl) {
        this.parcelBaseUrl = parcelBaseUrl;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public Map<String, String> getVmsToRegions() {
        return vmsToRegions;
    }

    public void setVmsToRegions(Map<String, String> vmsToRegions) {
        this.vmsToRegions = vmsToRegions;
    }
}
