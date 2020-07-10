package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class TagsV4Response implements JsonEntity, TaggedResponse {

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

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(application.get(key))
                .or(() -> Optional.ofNullable(userDefined.get(key)))
                .or(() -> Optional.ofNullable(defaults.get(key)))
                .orElse(null);
    }
}
