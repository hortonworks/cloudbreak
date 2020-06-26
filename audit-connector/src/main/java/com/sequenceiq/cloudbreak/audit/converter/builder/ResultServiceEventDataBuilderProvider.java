package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class ResultServiceEventDataBuilderProvider {

    public AuditProto.ResultServiceEventData.Builder getNewResultServiceEventDataBuilder() {
        return AuditProto.ResultServiceEventData.newBuilder();
    }

}
