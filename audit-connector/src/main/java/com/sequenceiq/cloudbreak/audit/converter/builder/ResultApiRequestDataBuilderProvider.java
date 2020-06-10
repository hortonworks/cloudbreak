package com.sequenceiq.cloudbreak.audit.converter.builder;

import com.cloudera.thunderhead.service.audit.AuditProto;
import org.springframework.stereotype.Component;

@Component
public class ResultApiRequestDataBuilderProvider {

    public AuditProto.ResultApiRequestData.Builder getNewResultApiRequestDataBuilder() {
        return AuditProto.ResultApiRequestData.newBuilder();
    }

}
