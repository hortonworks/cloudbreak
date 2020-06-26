package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.DATAHUB_RESOURCE_TYPE;

public enum  StackType {
    WORKLOAD,
    DATALAKE,
    TEMPLATE;

    public String getResourceType() {
        if (WORKLOAD.equals(this)) {
            return DATAHUB_RESOURCE_TYPE;
        }
        return name().toLowerCase();
    }
}
