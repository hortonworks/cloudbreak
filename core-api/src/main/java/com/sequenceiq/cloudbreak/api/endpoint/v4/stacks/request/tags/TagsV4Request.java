package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class TagsV4Request implements JsonEntity {

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private Map<String, String> applicationTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private Map<String, String> userDefinedTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
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
