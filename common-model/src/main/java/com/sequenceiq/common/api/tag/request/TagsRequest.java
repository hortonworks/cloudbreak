package com.sequenceiq.common.api.tag.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.common.api.tag.base.TagsBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "TagsRequest")
public class TagsRequest extends TagsBase {

    public TagsRequest() {
    }

    @JsonCreator
    public TagsRequest(Map<String, String> values) {
        super(values);
    }

    public TagsRequest(TagsBase tags) {
        super(tags);
    }
}
