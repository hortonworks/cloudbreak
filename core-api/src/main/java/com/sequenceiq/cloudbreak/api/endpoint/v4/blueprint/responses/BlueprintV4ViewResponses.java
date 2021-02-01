package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class BlueprintV4ViewResponses extends GeneralCollectionV4Response<BlueprintV4ViewResponse> {

    public BlueprintV4ViewResponses(Set<BlueprintV4ViewResponse> responses) {
        super(responses);
    }

    public BlueprintV4ViewResponses() {
        super(Sets.newHashSet());
    }
}
