package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralListV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;

public class AutoscaleStackV4Responses extends GeneralListV4Response<AutoscaleStackV4Response> {

    public AutoscaleStackV4Responses(List<AutoscaleStackV4Response> responses) {
        super(responses);
    }
}
