package com.sequenceiq.cloudbreak.cmtemplate;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.type.Versioned;

import java.util.Objects;
import java.util.Optional;

public enum BlackListedDownScaleRole implements EntitledForServiceScale {
    KAFKA_BROKER(Entitlement.DATAHUB_STREAMING_SCALING),
    NIFI_NODE(Entitlement.DATAHUB_FLOW_SCALING),
    ZEPPELIN_SERVER(Entitlement.DATAHUB_DEFAULT_SCALING),
    NAMENODE(Entitlement.DATAHUB_DEFAULT_SCALING);

    private final Entitlement entitledFor;

    private final Optional<String> blockedUntilCDPVersion;

    BlackListedDownScaleRole(Entitlement entitledFor) {
        this(entitledFor, null);
    }

    BlackListedDownScaleRole(Entitlement entitledFor, String blockedUntilCDPVersion) {
        this.entitledFor = Objects.requireNonNull(entitledFor);
        this.blockedUntilCDPVersion = Optional.ofNullable(blockedUntilCDPVersion);
    }

    @Override
    public Entitlement getEntitledFor() {
        return entitledFor;
    }

    @Override
    public Optional<String> getBlockedUntilCDPVersion() {
        return blockedUntilCDPVersion;
    }

    public Versioned getBlockedUntilCDPVersionAsVersion() {
        return () -> blockedUntilCDPVersion.orElse(null);
    }

}
