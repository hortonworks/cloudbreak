package com.sequenceiq.distrox.api.v1.distrox.model.tags;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.tag.request.TagsRequest;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModelProperty;

public class TagsV1Request implements JsonEntity {

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private TagsRequest application = new TagsRequest();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private TagsRequest userDefined = new TagsRequest();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
    private TagsRequest defaults = new TagsRequest();

    public TagsRequest getApplication() {
        return application;
    }

    public void setApplication(TagsRequest application) {
        this.application = application;
    }

    public TagsRequest getUserDefined() {
        return userDefined;
    }

    public void setUserDefined(TagsRequest userDefined) {
        this.userDefined = userDefined;
    }

    public TagsRequest getDefaults() {
        return defaults;
    }

    public void setDefaults(TagsRequest defaults) {
        this.defaults = defaults;
    }
}
