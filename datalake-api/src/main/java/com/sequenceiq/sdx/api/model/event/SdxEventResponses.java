package com.sequenceiq.sdx.api.model.event;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class SdxEventResponses extends GeneralCollectionV4Response<SdxEventResponse> {

    public SdxEventResponses(List<SdxEventResponse> responses) {
        super(responses);
    }

    public SdxEventResponses() {
        super(Lists.newArrayList());
    }
}
