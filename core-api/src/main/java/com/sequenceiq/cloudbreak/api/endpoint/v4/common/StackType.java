package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.DATAHUB_RESOURCE_TYPE;
import static com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService.LEGACY_RESOURCE_TYPE;

import java.util.Locale;

public enum  StackType {
    WORKLOAD,
    DATALAKE,
    TEMPLATE,
    LEGACY;

    public String getResourceType() {
        if (WORKLOAD.equals(this)) {
            return DATAHUB_RESOURCE_TYPE;
        }
        if (LEGACY.equals(this)) {
            return LEGACY_RESOURCE_TYPE;
        }
        return name().toLowerCase(Locale.ROOT);
    }
}
