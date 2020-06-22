package com.sequenceiq.it.util;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.tag.request.TagsRequest;

@Component
public class TagAdderUtil {

    private static final int GCP_TAG_MAX_LENGTH = 63;

    public void addTags(TagsRequest tags, String tagKey, String tagValue) {
        tagKey = applyLengthRestrictions(tagKey);
        tagValue = applyLengthRestrictions(tagValue);
        tags.addTag(tagKey, tagValue);
    }

    public void addTestNameTag(TagsRequest tags, String testName) {
        addTags(tags, "test-name", testName);
    }

    // TODO remove
    public void addTestNameTag(Map<String, String> tags, String testName) {
//        addTags(tags, "test-name", testName);
    }

    private String applyLengthRestrictions(String tag) {
        if (tag.length() > GCP_TAG_MAX_LENGTH) {
            tag = tag.substring(0, GCP_TAG_MAX_LENGTH - 1);
        }
        return tag;
    }
}
