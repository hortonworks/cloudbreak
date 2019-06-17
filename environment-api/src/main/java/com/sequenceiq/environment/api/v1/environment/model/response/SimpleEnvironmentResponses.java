package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "SimpleEnvironmentV1Responses", description = "Wrapper which contains multiple SimpleEnvironmentV1Responses")
public class SimpleEnvironmentResponses extends GeneralCollectionV1Response<SimpleEnvironmentResponse> {
    public SimpleEnvironmentResponses(Collection<SimpleEnvironmentResponse> responses) {
        super(responses);
    }

    public SimpleEnvironmentResponses() {
        super(Sets.newHashSet());
    }
}
