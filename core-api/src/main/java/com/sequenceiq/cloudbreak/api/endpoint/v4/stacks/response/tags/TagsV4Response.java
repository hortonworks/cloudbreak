package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags;

import java.util.Optional;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.api.tag.response.TagsResponse;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class TagsV4Response implements JsonEntity, TaggedResponse {

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private TagsResponse application = new TagsResponse();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private TagsResponse userDefined = new TagsResponse();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
    private TagsResponse defaults = new TagsResponse();

    public TagsResponse getApplication() {
        return application;
    }

    public void setApplication(TagsResponse application) {
        this.application = application;
    }

    public TagsResponse getUserDefined() {
        return userDefined;
    }

    public void setUserDefined(TagsResponse userDefined) {
        this.userDefined = userDefined;
    }

    public TagsResponse getDefaults() {
        return defaults;
    }

    public void setDefaults(TagsResponse defaults) {
        this.defaults = defaults;
    }

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(application.getTagValue(key))
                .or(() -> Optional.ofNullable(userDefined.getTagValue(key)))
                .or(() -> Optional.ofNullable(defaults.getTagValue(key)))
                .orElse(null);
    }
}
