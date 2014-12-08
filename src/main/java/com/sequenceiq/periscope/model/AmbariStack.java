package com.sequenceiq.periscope.model;

import com.sequenceiq.periscope.domain.Ambari;

public final class AmbariStack {

    private final Ambari ambari;
    private final Long stackId;

    public AmbariStack(Ambari ambari) {
        this(ambari, null);
    }

    public AmbariStack(Ambari ambari, Long stackId) {
        this.ambari = ambari;
        this.stackId = stackId;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public Long getStackId() {
        return stackId;
    }
}
