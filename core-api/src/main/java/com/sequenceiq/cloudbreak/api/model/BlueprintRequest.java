package com.sequenceiq.cloudbreak.api.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.BlueprintModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueprintRequest extends BlueprintBase {
    @ApiModelProperty(value = BlueprintModelDescription.URL)
    private String url;

    @ApiModelProperty(value = ModelDescriptions.BlueprintModelDescription.BLUEPRINT_PROPERTIES)
    private List<Map<String, Map<String, String>>> properties;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Map<String, Map<String, String>>> getProperties() {
        return properties;
    }

    public void setProperties(List<Map<String, Map<String, String>>> properties) {
        this.properties = properties;
    }
}
