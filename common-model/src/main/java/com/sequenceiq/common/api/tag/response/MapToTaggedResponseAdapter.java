package com.sequenceiq.common.api.tag.response;

import java.util.Map;

public class MapToTaggedResponseAdapter implements TaggedResponse {

    private final Map<String, String> tags;

    public MapToTaggedResponseAdapter(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String getTagValue(String key) {
        return tags.get(key);
    }
}
