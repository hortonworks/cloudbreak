package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.common.api.tag.response.TagsResponse;

public class TagResponse {

    private TagsResponse userDefined = new TagsResponse();

    private TagsResponse defaults = new TagsResponse();

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
}
