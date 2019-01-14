package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralListV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CloudbreakEventV4Responses extends GeneralListV4Response<CloudbreakEventV4Response> {

    public CloudbreakEventV4Responses(List<CloudbreakEventV4Response> responses) {
        super(responses);
    }
}
