package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

public class StackEndpointV4Responses  extends GeneralCollectionV4Response<StackEndpointV4Response> {

    public StackEndpointV4Responses(Set<StackEndpointV4Response> responses) {
        super(responses);
    }

    public StackEndpointV4Responses() {
        super(List.of());
    }
}
