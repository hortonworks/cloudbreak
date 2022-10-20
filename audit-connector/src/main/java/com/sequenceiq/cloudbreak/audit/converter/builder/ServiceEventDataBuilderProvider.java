package com.sequenceiq.cloudbreak.audit.converter.builder;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;

@Component
public class ServiceEventDataBuilderProvider {

    public AuditProto.ServiceEventData.Builder getNewServiceEventDataBuilder() {
        return AuditProto.ServiceEventData.newBuilder();
    }

}
