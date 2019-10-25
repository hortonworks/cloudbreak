package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.common.api.type.CdpResourceType;

@Component
public class CdpResourceTypeProvider {

    public CdpResourceType fromStackType(StackType stackType) {
        if (stackType == null) {
            return CdpResourceType.DEFAULT;
        }
        switch (stackType) {
            case WORKLOAD:
                return CdpResourceType.DATAHUB;
            case DATALAKE:
                return CdpResourceType.DATALAKE;
            default:
                return CdpResourceType.DEFAULT;
        }
    }
}
