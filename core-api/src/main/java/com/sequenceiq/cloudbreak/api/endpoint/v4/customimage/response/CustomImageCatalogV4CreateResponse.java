package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class CustomImageCatalogV4CreateResponse implements JsonEntity {

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
