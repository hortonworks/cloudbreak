package com.sequenceiq.environment.api.v1.credential.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "CredentialV1Responses", description = "Wrapper which contains multiple CredentialV1Response")
public class CredentialResponses extends GeneralCollectionV1Response<CredentialResponse> {
    public CredentialResponses(Set<CredentialResponse> responses) {
        super(responses);
    }

    public CredentialResponses() {
        super(Sets.newHashSet());
    }
}
