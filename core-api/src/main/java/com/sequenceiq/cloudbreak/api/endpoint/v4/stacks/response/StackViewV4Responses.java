package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

public class StackViewV4Responses extends GeneralCollectionV4Response<StackViewV4Response> {

    public StackViewV4Responses(Set<StackViewV4Response> responses) {
        super(responses);
    }

    public StackViewV4Responses() {
        super(List.of());
    }
}
