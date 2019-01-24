package com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ManagementPackV4Responses extends GeneralSetV4Response<ManagementPackV4Response> {
    public ManagementPackV4Responses(Set<ManagementPackV4Response> responses) {
        super(responses);
    }

    public ManagementPackV4Responses() {
        super(Sets.newHashSet());
    }
}
