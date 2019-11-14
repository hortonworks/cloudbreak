package com.sequenceiq.it.util;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class TagAdderUtil {

    private static final int GCP_TAG_MAX_LENGTH = 63;

    public void addTags(Map<String, String> tagCollection, String tagKey, String tagValue) {
        tagKey = applyLengthRestrictions(tagKey);
        tagValue = applyLengthRestrictions(tagValue);
        tagCollection.put(tagKey, tagValue);
    }

    public void addTestNameTag(Map<String, String> tagCollection, String testName) {
        addTags(tagCollection, "test-name", testName);
    }

    private String applyLengthRestrictions(String tag) {
        if (tag.length() > GCP_TAG_MAX_LENGTH) {
            tag = tag.substring(0, GCP_TAG_MAX_LENGTH - 1);
        }
        return tag;
    }
}
