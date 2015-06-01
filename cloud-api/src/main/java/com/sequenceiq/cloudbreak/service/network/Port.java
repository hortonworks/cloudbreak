package com.sequenceiq.cloudbreak.service.network;

import java.util.List;

public class Port {

    private final String name;
    private final String localPort;
    private final ExposedService exposedService;
    private final String port;
    private final String protocol;
    private final List<EndpointRule> aclRules;

    public Port(ExposedService exposedService, String port, String localPort, String protocol, List<EndpointRule> aclRules) {
        this.localPort = localPort;
        this.port = port;
        this.name = exposedService.getPortName();
        this.protocol = protocol;
        this.aclRules = aclRules;
        this.exposedService = exposedService;
    }

    public String getLocalPort() {
        return localPort;
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

    public String getName() {
        return name;
    }

    public ExposedService getExposedService() {
        return exposedService;
    }
}
