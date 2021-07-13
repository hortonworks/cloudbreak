package com.sequenceiq.cloudbreak.cmtemplate;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.type.Versioned;

import java.util.Optional;

public interface EntitledForServiceScale {

    String name();

    Entitlement getEntitledFor();

    Optional<String> getBlockedUntilCDPVersion();

    Optional<String> getRequiredService();

    Versioned getBlockedUntilCDPVersionAsVersion();

}
