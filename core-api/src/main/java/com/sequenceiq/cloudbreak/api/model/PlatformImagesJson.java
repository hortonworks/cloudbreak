package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformImagesJson implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.IMAGES)
    private Map<String, Map<String, String>> images;

    @ApiModelProperty(ConnectorModelDescription.IMAGES_REGEX)
    private Map<String, String> imagesRegex;

    public Map<String, Map<String, String>> getImages() {
        return images;
    }

    public void setImages(Map<String, Map<String, String>> images) {
        this.images = images;
    }

    public Map<String, String> getImagesRegex() {
        return imagesRegex;
    }

    public void setImagesRegex(Map<String, String> imagesRegex) {
        this.imagesRegex = imagesRegex;
    }
}
