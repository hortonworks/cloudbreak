package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "CredentialResponses", description = "Wrapper which contains multiple CredentialV4Response")
public class CredentialV4Responses extends GeneralCollectionV4Response<CredentialV4Response> {
    public CredentialV4Responses(Set<CredentialV4Response> responses) {
        super(responses);
    }

    public CredentialV4Responses() {
        super(Sets.newHashSet());
    }
}
