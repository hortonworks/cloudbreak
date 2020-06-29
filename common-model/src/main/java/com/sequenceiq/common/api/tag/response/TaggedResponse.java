package com.sequenceiq.common.api.tag.response;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface TaggedResponse {

    @JsonIgnore
    String getTagValue(String key);
}
