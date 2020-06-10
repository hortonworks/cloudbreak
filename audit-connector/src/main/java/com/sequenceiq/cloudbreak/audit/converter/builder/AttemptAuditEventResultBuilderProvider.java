package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class AttemptAuditEventResultBuilderProvider {

    public AuditProto.AttemptAuditEventResult.Builder getNewAttemptAuditEventResultBuilder() {
        return AuditProto.AttemptAuditEventResult.newBuilder();
    }

}
