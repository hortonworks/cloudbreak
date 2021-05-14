package com.sequenceiq.distrox.api.v1.distrox.model.event;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DistroXEventV1Responses extends GeneralCollectionV4Response<DistroXEventV1Response> {

    public DistroXEventV1Responses(List<DistroXEventV1Response> responses) {
        super(responses);
    }

    public DistroXEventV1Responses() {
        super(Lists.newArrayList());
    }
}
