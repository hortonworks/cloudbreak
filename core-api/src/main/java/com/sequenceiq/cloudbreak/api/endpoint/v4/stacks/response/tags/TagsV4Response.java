package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class TagsV4Response implements JsonEntity {

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private Map<String, String> application = new HashMap<>();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private Map<String, String> userDefined = new HashMap<>();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
    private Map<String, String> defaults = new HashMap<>();

    public Map<String, String> getApplication() {
        return application;
    }

    public void setApplication(Map<String, String> application) {
        this.application = application;
    }

    public Map<String, String> getUserDefined() {
        return userDefined;
    }

    public void setUserDefined(Map<String, String> userDefined) {
        this.userDefined = userDefined;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }
}
