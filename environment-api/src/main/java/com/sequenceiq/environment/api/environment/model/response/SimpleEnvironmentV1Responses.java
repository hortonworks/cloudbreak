package com.sequenceiq.environment.api.environment.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SimpleEnvironmentV1Responses extends GeneralCollectionV1Response<SimpleEnvironmentV1Response> {
    public SimpleEnvironmentV1Responses(Set<SimpleEnvironmentV1Response> responses) {
        super(responses);
    }

    public SimpleEnvironmentV1Responses() {
        super(Sets.newHashSet());
    }
}
