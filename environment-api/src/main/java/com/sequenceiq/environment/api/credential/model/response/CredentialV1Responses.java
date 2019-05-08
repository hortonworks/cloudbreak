package com.sequenceiq.environment.api.credential.model.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "CredentialResponses", description = "Wrapper which contains multiple CredentialV1Response")
public class CredentialV1Responses extends GeneralCollectionV1Response<CredentialV1Response> {
    public CredentialV1Responses(Set<CredentialV1Response> responses) {
        super(responses);
    }

    public CredentialV1Responses() {
        super(Sets.newHashSet());
    }
}
