package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClusterTemplateViewV4Responses extends GeneralCollectionV4Response<ClusterTemplateViewV4Response> {

    public ClusterTemplateViewV4Responses(Set<ClusterTemplateViewV4Response> responses) {
        super(responses);
    }

    public ClusterTemplateViewV4Responses() {
        super(Sets.newHashSet());
    }
}
