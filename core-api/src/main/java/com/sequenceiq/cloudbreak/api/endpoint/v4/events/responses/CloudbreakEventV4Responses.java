package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CloudbreakEventV4Responses extends GeneralCollectionV4Response<CloudbreakEventV4Response> {

    public CloudbreakEventV4Responses(List<CloudbreakEventV4Response> responses) {
        super(responses);
    }

    public CloudbreakEventV4Responses() {
        super(Lists.newArrayList());
    }
}
