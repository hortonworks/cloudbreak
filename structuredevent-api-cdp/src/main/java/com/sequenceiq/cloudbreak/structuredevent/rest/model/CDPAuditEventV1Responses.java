package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.sequenceiq.common.model.annotations.Immutable;

import io.swagger.annotations.ApiModel;

@Immutable
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPAuditEventV1Responses extends CDPStructuredEventGeneralCollectionV1Response<CDPAuditEventV1Response> {

    public CDPAuditEventV1Responses(List<CDPAuditEventV1Response> responses) {
        super(responses);
    }

    public CDPAuditEventV1Responses() {
        super(Lists.newArrayList());
    }
}
