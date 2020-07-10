package com.sequenceiq.common.api.tag.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.common.api.tag.base.TagsBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "TagsResponse")
public class TagsResponse extends TagsBase {

    public TagsResponse() {
    }

    @JsonCreator
    public TagsResponse(Map<String, String> values) {
        super(values);
    }

    public TagsResponse(TagsBase tags) {
        super(tags);
    }
}
