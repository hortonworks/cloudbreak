package com.sequenceiq.environment.api.v1.tags.model.response;

import java.util.ArrayList;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedAccountTagResponses extends GeneralCollectionV1Response<GeneratedAccountTagResponse> {

    public GeneratedAccountTagResponses() {
        super(new ArrayList<>());
    }

    public GeneratedAccountTagResponses(Set<GeneratedAccountTagResponse> responses) {
        super(responses);
    }
}
