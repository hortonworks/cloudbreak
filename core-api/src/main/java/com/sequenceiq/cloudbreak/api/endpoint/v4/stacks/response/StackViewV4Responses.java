package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

public class StackViewV4Responses extends GeneralSetV4Response<StackViewV4Response> {

    public StackViewV4Responses(Set<StackViewV4Response> responses) {
        super(responses);
    }
}
