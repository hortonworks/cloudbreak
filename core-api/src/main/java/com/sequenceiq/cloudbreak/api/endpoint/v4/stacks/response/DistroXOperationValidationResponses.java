package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

public class DistroXOperationValidationResponses extends GeneralCollectionV4Response<DistroXOperationValidationResponse> {

    public DistroXOperationValidationResponses(Set<DistroXOperationValidationResponse> responses) {
        super(responses);
    }

    public DistroXOperationValidationResponses() {
        super(List.of());
    }

    @Override
    public String toString() {
        return "DistroXOperationValidationResponses{" +
                "responses=" + getResponses() +
                '}';
    }
}
