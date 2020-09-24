package com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;
import com.sequenceiq.common.model.annotations.Immutable;

import io.swagger.annotations.ApiModel;

@Immutable
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEventV4Responses extends GeneralCollectionV4Response<AuditEventV4Response> {

    public AuditEventV4Responses(List<AuditEventV4Response> responses) {
        super(responses);
    }

    public AuditEventV4Responses() {
        super(Lists.newArrayList());
    }
}
