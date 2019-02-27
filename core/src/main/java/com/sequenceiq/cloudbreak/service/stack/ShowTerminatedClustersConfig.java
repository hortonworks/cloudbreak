package com.sequenceiq.cloudbreak.service.stack;

import java.time.Duration;

public class ShowTerminatedClustersConfig {

    public enum Source {
        GLOBAL,
        USER
    }

    private final Source source;

    private final Boolean active;

    private final Duration timeout;

    public ShowTerminatedClustersConfig(Boolean active, Duration timeout, boolean userDefined) {
        this.active = active;
        this.timeout = timeout;
        source = userDefined ? Source.USER : Source.GLOBAL;
    }

    public Boolean isActive() {
        return active;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Source getSource() {
        return source;
    }
}
