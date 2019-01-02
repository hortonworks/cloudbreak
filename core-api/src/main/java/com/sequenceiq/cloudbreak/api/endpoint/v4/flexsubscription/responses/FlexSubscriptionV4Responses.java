package com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexSubscriptionV4Responses extends GeneralSetV4Response<FlexSubscriptionV4Response> {
    public FlexSubscriptionV4Responses(Set<FlexSubscriptionV4Response> responses) {
        super(responses);
    }
}
