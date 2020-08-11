package com.sequenceiq.cloudbreak.structuredevent.controller.audits.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.structuredevent.controller.CDPStructuredEventGeneralCollectionV4Response;
import com.sequenceiq.cloudbreak.structuredevent.controller.Immutable;

import io.swagger.annotations.ApiModel;

@Immutable
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPAuditEventV4Responses extends CDPStructuredEventGeneralCollectionV4Response<CDPAuditEventV4Response> {

    public CDPAuditEventV4Responses(List<CDPAuditEventV4Response> responses) {
        super(responses);
    }

    public CDPAuditEventV4Responses() {
        super(Lists.newArrayList());
    }
}
