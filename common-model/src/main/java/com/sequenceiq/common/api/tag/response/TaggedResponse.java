package com.sequenceiq.common.api.tag.response;

import java.util.Optional;

public interface TaggedResponse {

    Optional<TagsResponse> getTagsResponse();
}
