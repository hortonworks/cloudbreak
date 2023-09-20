package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.Locale;

public class AzurePortView {

    private final String cidr;

    private final String port;

    private final String protocol;

    public AzurePortView(String cidr, String port, String protocol) {
        this.cidr = cidr;
        this.port = port;
        this.protocol = protocol;
    }

    public String getCidr() {
        return cidr;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCapitalProtocol() {
        return protocol.substring(0, 1).toUpperCase(Locale.ROOT) + protocol.substring(1);
    }
}
