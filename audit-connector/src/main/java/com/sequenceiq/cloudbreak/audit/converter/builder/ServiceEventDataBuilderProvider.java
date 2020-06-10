package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class ServiceEventDataBuilderProvider {

    public AuditProto.ServiceEventData.Builder getNewServiceEventDataBuilder() {
        return AuditProto.ServiceEventData.newBuilder();
    }

}
