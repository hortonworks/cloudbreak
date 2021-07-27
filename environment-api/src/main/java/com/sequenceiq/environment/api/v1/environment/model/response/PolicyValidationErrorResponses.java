package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.annotations.ApiModel;

@ApiModel("PolicyValidationErrorResponses")
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
