package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Optional;

import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.common.api.tag.response.TagsResponse;

public class TagResponse implements TaggedResponse {

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

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(userDefined.getTagValue(key))
                .or(() -> Optional.ofNullable(defaults.getTagValue(key)))
                .orElse(null);
    }
}
