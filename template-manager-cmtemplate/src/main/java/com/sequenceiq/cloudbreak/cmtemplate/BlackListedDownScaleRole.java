package com.sequenceiq.cloudbreak.cmtemplate;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

public enum BlackListedDownScaleRole {
    KAFKA_BROKER(EntitlementService.DATAHUB_STREAMING_SCALING),
    NIFI_NODE(EntitlementService.DATAHUB_FLOW_SCALING),
    ZEPPELIN_SERVER(EntitlementService.DATAHUB_DEFAULT_SCALING),
    NAMENODE(EntitlementService.DATAHUB_DEFAULT_SCALING);

    private final String entitledFor;

    BlackListedDownScaleRole(String entitledFor) {
        this.entitledFor = entitledFor;
    }

    public String getEntitledFor() {
        return entitledFor;
    }
}
