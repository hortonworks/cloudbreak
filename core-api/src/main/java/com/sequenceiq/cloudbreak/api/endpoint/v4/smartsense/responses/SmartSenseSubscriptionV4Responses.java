package com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartSenseSubscriptionV4Responses extends GeneralSetV4Response<SmartSenseSubscriptionV4Response> {
    public SmartSenseSubscriptionV4Responses(Set<SmartSenseSubscriptionV4Response> responses) {
        super(responses);
    }

    public SmartSenseSubscriptionV4Responses() {
        super(Sets.newHashSet());
    }
}
