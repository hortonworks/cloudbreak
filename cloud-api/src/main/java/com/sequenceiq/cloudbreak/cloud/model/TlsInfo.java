package com.sequenceiq.cloudbreak.cloud.model;

public class TlsInfo {

    private boolean usePrivateIpToTls;

    public TlsInfo(boolean usePrivateIpToTls) {
        this.usePrivateIpToTls = usePrivateIpToTls;
    }

    public boolean usePrivateIpToTls() {
        return usePrivateIpToTls;
    }
}
