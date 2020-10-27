package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Objects;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;

public enum BlackListedUpScaleRole {
    KAFKA_BROKER(Entitlement.DATAHUB_STREAMING_SCALING),
    NIFI_NODE(Entitlement.DATAHUB_FLOW_SCALING),
    ZEPPELIN_SERVER(Entitlement.DATAHUB_DEFAULT_SCALING),
    NAMENODE(Entitlement.DATAHUB_DEFAULT_SCALING);

    private final Entitlement entitledFor;

    BlackListedUpScaleRole(Entitlement entitledFor) {
        this.entitledFor = Objects.requireNonNull(entitledFor);
    }

    public Entitlement getEntitledFor() {
        return entitledFor;
    }
}
