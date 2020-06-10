package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class ApiRequestDataBuilderProvider {

    public AuditProto.ApiRequestData.Builder getNewApiRequestDataBuilder() {
        return AuditProto.ApiRequestData.newBuilder();
    }

}
