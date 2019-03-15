package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ClusterTemplateV4Responses extends GeneralCollectionV4Response<ClusterTemplateV4Response> {

    public ClusterTemplateV4Responses(Set<ClusterTemplateV4Response> responses) {
        super(responses);
    }

    public ClusterTemplateV4Responses() {
        super(new HashSet<>());
    }
}
