package com.sequenceiq.periscope.model;

import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.SecurityConfig;

public final class AmbariStack {

    private final Ambari ambari;

    private final Long stackId;

    private final SecurityConfig securityConfig;

    public AmbariStack(Ambari ambari) {
        this(ambari, null, null);
    }

    public AmbariStack(Ambari ambari, Long stackId, SecurityConfig securityConfig) {
        this.ambari = ambari;
        this.stackId = stackId;
        this.securityConfig = securityConfig;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public Long getStackId() {
        return stackId;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }
}
