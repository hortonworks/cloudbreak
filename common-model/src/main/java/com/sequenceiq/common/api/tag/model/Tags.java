package com.sequenceiq.common.api.tag.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.common.api.tag.base.TagsBase;

public class Tags extends TagsBase {

    public Tags() {
    }

    @JsonCreator
    public Tags(Map<String, String> values) {
        super(values);
    }

    public Tags(TagsBase tags) {
        super(tags);
    }
}
