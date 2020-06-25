package com.sequenceiq.common.api.tag.response;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface TaggedResponse {

    @JsonIgnore
    Optional<TagsResponse> getTagsResponse();
}
