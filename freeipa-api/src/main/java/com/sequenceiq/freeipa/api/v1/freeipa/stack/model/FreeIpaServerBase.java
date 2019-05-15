package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import javax.validation.constraints.NotNull;

public abstract class FreeIpaServerBase {

    @NotNull
    private String domain;

    @NotNull
    private String hostname;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
