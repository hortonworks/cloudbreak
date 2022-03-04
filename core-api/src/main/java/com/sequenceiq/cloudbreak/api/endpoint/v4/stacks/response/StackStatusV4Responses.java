package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

public class StackStatusV4Responses extends GeneralCollectionV4Response<StackStatusV4Response> {

    public StackStatusV4Responses(Set<StackStatusV4Response> responses) {
        super(responses);
    }

    public StackStatusV4Responses() {
        super(List.of());
    }
}
