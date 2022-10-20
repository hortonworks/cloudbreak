package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ImageBasicInfoV4Response {

    @JsonProperty("uuid")
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "ImageBasicInfoV4Response{" +
                "uuid='" + uuid + '\'' +
                '}';
    }
}
