package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ClusterDefinitionV4ViewResponses extends GeneralCollectionV4Response<ClusterDefinitionV4ViewResponse> {

    public ClusterDefinitionV4ViewResponses(Set<ClusterDefinitionV4ViewResponse> responses) {
        super(responses);
    }

    public ClusterDefinitionV4ViewResponses() {
        super(Sets.newHashSet());
    }
}
