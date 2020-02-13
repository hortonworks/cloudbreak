package com.sequenceiq.environment.api.v1.tags.model.response;

import java.util.ArrayList;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTagResponses extends GeneralCollectionV1Response<AccountTagResponse> {

    public AccountTagResponses() {
        super(new ArrayList<>());
    }

    public AccountTagResponses(Set<AccountTagResponse> responses) {
        super(responses);
    }
}
