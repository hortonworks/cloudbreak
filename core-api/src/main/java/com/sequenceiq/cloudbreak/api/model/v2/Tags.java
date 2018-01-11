package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tags implements JsonEntity {
    @ApiModelProperty(ModelDescriptions.StackModelDescription.APPLICATION_TAGS)
    private Map<String, String> applicationTags = new HashMap<>();

    @ApiModelProperty(ModelDescriptions.StackModelDescription.USERDEFINED_TAGS)
    private Map<String, String> userDefinedTags = new HashMap<>();

    @ApiModelProperty(ModelDescriptions.StackModelDescription.DEFAULT_TAGS)
    private Map<String, String> defaultTags = new HashMap<>();

    public Map<String, String> getApplicationTags() {
        return applicationTags;
    }

    public void setApplicationTags(Map<String, String> applicationTags) {
        this.applicationTags = applicationTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public void setUserDefinedTags(Map<String, String> userDefinedTags) {
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
    }
}
