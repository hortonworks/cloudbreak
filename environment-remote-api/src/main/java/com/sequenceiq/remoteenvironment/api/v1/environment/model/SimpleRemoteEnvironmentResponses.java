package com.sequenceiq.remoteenvironment.api.v1.environment.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import com.sequenceiq.remoteenvironment.api.v1.GeneralCollectionV1Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SimpleRemoteEnvironmentV1Responses", description = "Wrapper which contains multiple SimpleRemoteEnvironmentV1Responses")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleRemoteEnvironmentResponses extends GeneralCollectionV1Response<SimpleRemoteEnvironmentResponse> {
    public SimpleRemoteEnvironmentResponses(Collection<SimpleRemoteEnvironmentResponse> responses) {
        super(responses);
    }

    public SimpleRemoteEnvironmentResponses() {
        super(Sets.newHashSet());
    }
}
