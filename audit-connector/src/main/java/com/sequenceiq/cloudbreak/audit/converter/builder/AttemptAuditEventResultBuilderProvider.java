package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class AttemptAuditEventResultBuilderProvider {

    public AuditProto.AttemptAuditEventResult.Builder getNewAttemptAuditEventResultBuilder() {
        return AuditProto.AttemptAuditEventResult.newBuilder();
    }

}
