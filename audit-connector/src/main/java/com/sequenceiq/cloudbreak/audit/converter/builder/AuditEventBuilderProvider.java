package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class AuditEventBuilderProvider {

    public AuditProto.AuditEvent.Builder getNewAuditEventBuilder() {
        return AuditProto.AuditEvent.newBuilder();
    }

}
