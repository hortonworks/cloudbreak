package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class AuditEventBuilderProvider {

    public AuditProto.AuditEvent.Builder getNewAuditEventBuilder() {
        return AuditProto.AuditEvent.newBuilder();
    }

}
