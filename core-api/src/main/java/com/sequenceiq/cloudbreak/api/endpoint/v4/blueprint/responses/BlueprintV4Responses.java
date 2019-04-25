package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class BlueprintV4Responses extends GeneralCollectionV4Response<BlueprintV4Response> {

    public BlueprintV4Responses(Set<BlueprintV4Response> responses) {
        super(responses);
    }

    public BlueprintV4Responses() {
        super(new HashSet<>());
    }
}
