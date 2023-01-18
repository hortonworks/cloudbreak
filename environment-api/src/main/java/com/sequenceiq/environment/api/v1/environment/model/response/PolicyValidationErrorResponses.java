package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PolicyValidationErrorResponses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyValidationErrorResponses extends GeneralCollectionV1Response<PolicyValidationErrorResponse> {

    public PolicyValidationErrorResponses(Collection<PolicyValidationErrorResponse> responses) {
        super(responses);
    }

    public PolicyValidationErrorResponses() {
        super(Sets.newHashSet());
    }

    @Override
    public String toString() {
        return super.toString() + ", PolicyValidationErrorResponses{}";
    }
}
