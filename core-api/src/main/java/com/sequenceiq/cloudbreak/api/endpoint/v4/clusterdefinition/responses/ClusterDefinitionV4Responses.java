package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ClusterDefinitionV4Responses extends GeneralCollectionV4Response<ClusterDefinitionV4Response> {

    public ClusterDefinitionV4Responses(Set<ClusterDefinitionV4Response> responses) {
        super(responses);
    }

    public ClusterDefinitionV4Responses() {
        super(new HashSet<>());
    }
}
