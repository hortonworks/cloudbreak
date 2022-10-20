package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.Optional;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.type.Versioned;

public interface EntitledForServiceScale {

    String name();

    Entitlement getEntitledFor();

    Optional<String> getBlockedUntilCDPVersion();

    Optional<String> getRequiredService();

    Versioned getBlockedUntilCDPVersionAsVersion();

}
