package com.sequenceiq.cloudbreak.service.network;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.EndpointRule;

public class Port {

    private final String name;

    private final String localPort;

    private final ExposedService exposedService;

    private final String port;

    private final String protocol;

    private final String knoxUrl;

    private final List<EndpointRule> aclRules;

    public Port(ExposedService exposedService, String port, String protocol, String knoxUrl) {
        this(exposedService, port, port, protocol, new ArrayList<>(), knoxUrl);
    }

    public Port(ExposedService exposedService, String port, String localPort, String protocol, List<EndpointRule> aclRules, String knoxUrl) {
        this.localPort = localPort;
        this.port = port;
        this.name = exposedService.getPortName();
        this.protocol = protocol;
        this.aclRules = aclRules;
        this.exposedService = exposedService;
        this.knoxUrl = knoxUrl;
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

    public String getKnoxUrl() {
        return knoxUrl;
    }
}
