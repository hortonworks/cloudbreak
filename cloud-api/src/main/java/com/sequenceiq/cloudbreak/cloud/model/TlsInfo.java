package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TlsInfo {

    private final boolean usePrivateIpToTls;

    @JsonCreator
    public TlsInfo(@JsonProperty("usePrivateIpToTls") boolean usePrivateIpToTls) {
        this.usePrivateIpToTls = usePrivateIpToTls;
    }

    public boolean isUsePrivateIpToTls() {
        return usePrivateIpToTls;
    }
}
