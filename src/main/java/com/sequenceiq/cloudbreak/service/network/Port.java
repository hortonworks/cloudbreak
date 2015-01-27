package com.sequenceiq.cloudbreak.service.network;

import java.util.List;

public class Port {

    private final String localPort;
    private final String name;
    private final String port;
    private final String protocol;
    private final List<EndpointRule> aclRules;

    public Port(String name, String port, String localPort, String protocol, List<EndpointRule> aclRules) {
        this.name = name;
        this.localPort = localPort;
        this.port = port;
        this.protocol = protocol;
        this.aclRules = aclRules;
    }

    public String getLocalPort() {
        return localPort;
    }

    public String getName() {
        return name;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<EndpointRule> getAclRules() {
        return aclRules;
    }
}
