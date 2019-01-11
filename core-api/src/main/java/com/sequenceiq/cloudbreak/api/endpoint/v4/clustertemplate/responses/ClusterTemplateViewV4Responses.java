package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterTemplateViewV4Responses extends GeneralSetV4Response<ClusterTemplateViewV4Response> {

    public ClusterTemplateViewV4Responses(Set<ClusterTemplateViewV4Response> responses) {
        super(responses);
    }
}
