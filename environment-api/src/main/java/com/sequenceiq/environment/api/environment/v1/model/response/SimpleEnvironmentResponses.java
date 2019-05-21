package com.sequenceiq.environment.api.environment.v1.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SimpleEnvironmentResponses extends GeneralCollectionV1Response<SimpleEnvironmentResponse> {
    public SimpleEnvironmentResponses(Set<SimpleEnvironmentResponse> responses) {
        super(responses);
    }

    public SimpleEnvironmentResponses() {
        super(Sets.newHashSet());
    }
}
