package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import java.util.List;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;

public class AutoscaleStackV4Responses extends GeneralCollectionV4Response<AutoscaleStackV4Response> {

    public AutoscaleStackV4Responses(List<AutoscaleStackV4Response> responses) {
        super(responses);
    }

    public AutoscaleStackV4Responses() {
        super(Sets.newHashSet());
    }
}
