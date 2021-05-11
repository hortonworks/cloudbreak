package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4VmImageResponse implements JsonEntity {

    @JsonProperty
    private String region;

    @JsonProperty
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
