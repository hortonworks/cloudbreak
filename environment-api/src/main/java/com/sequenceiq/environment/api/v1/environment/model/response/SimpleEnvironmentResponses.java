package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SimpleEnvironmentV1Responses", description = "Wrapper which contains multiple SimpleEnvironmentV1Responses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleEnvironmentResponses extends GeneralCollectionV1Response<SimpleEnvironmentResponse> {
    public SimpleEnvironmentResponses(Collection<SimpleEnvironmentResponse> responses) {
        super(responses);
    }

    public SimpleEnvironmentResponses() {
        super(Sets.newHashSet());
    }
}
