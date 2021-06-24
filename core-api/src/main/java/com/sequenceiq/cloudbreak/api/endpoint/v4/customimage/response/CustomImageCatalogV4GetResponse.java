package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<String> imageIds;

    @JsonProperty
    private Set<CustomImageCatalogV4ImageListItemResponse> images = new HashSet<>();

    public Set<CustomImageCatalogV4ImageListItemResponse> getImages() {
        return images;
    }

    public void setImages(Set<CustomImageCatalogV4ImageListItemResponse> images) {
        this.images = images;
    }
}
