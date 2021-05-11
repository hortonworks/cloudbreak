package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4GetResponse extends CustomImageCatalogV4ListItemResponse {

    @JsonProperty
    private Set<String> imageIds = new HashSet<>();

    public Set<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(Set<String> imageIds) {
        this.imageIds = imageIds;
    }
}
