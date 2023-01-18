package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import java.util.List;

import com.google.common.collect.Lists;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CDPEventV1Responses extends CDPStructuredEventGeneralCollectionV1Response<CDPEventV1Response> {

    public CDPEventV1Responses(List<CDPEventV1Response> responses) {
        super(responses);
    }

    public CDPEventV1Responses() {
        super(Lists.newArrayList());
    }
}
