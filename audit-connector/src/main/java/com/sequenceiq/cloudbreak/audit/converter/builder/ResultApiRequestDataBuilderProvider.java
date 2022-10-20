package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class ResultApiRequestDataBuilderProvider {

    public AuditProto.ResultApiRequestData.Builder getNewResultApiRequestDataBuilder() {
        return AuditProto.ResultApiRequestData.newBuilder();
    }

}
