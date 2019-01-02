package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ExposedServiceV4Responses extends GeneralSetV4Response<ExposedServiceV4Response> {
    public ExposedServiceV4Responses(Set<ExposedServiceV4Response> responses) {
        super(responses);
    }
}
