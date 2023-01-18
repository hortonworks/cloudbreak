package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Deprecated
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxCustomClusterRequest extends SdxClusterRequestBase {

    public String getRuntime() {
        return null;
    }

    @Override
    @JsonProperty("imageSettingsV4Request")
    public ImageSettingsV4Request getImage() {
        return super.getImage();
    }

    @Override
    @JsonProperty("imageSettingsV4Request")
    public void setImage(ImageSettingsV4Request image) {
        super.setImage(image);
    }
}
