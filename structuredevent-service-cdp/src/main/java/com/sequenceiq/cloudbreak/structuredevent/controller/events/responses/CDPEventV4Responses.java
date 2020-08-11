package com.sequenceiq.cloudbreak.structuredevent.controller.events.responses;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventGeneralCollectionV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CDPEventV4Responses extends CDPStructuredEventGeneralCollectionV4Response<CDPEventV4Response> {

    public CDPEventV4Responses(List<CDPEventV4Response> responses) {
        super(responses);
    }

    public CDPEventV4Responses() {
        super(Lists.newArrayList());
    }
}
