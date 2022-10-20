package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class ApiRequestDataBuilderProvider {

    public AuditProto.ApiRequestData.Builder getNewApiRequestDataBuilder() {
        return AuditProto.ApiRequestData.newBuilder();
    }

}
