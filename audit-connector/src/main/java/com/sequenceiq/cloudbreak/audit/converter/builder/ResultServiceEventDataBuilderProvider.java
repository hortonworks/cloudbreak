package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class ResultServiceEventDataBuilderProvider {

    public AuditProto.ResultServiceEventData.Builder getNewResultServiceEventDataBuilder() {
        return AuditProto.ResultServiceEventData.newBuilder();
    }

}
