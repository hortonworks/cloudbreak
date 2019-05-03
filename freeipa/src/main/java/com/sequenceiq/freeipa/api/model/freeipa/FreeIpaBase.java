package com.sequenceiq.freeipa.api.model.freeipa;

import javax.validation.constraints.NotNull;

public abstract class FreeIpaBase {

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
