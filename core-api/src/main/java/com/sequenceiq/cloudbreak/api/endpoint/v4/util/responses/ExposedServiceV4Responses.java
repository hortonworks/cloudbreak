package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class ExposedServiceV4Responses extends GeneralCollectionV4Response<ExposedServiceV4Response> {
    public ExposedServiceV4Responses(Set<ExposedServiceV4Response> responses) {
        super(responses);
    }

    public ExposedServiceV4Responses() {
        super(Sets.newHashSet());
    }
}
