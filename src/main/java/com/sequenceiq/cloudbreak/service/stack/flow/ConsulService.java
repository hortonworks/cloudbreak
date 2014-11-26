package com.sequenceiq.cloudbreak.service.stack.flow;

import com.ecwid.consul.v1.ConsulClient;

public class ConsulService {

    private final ConsulClient consulClient;
    private final String serviceName;

    public ConsulService(ConsulClient consulClient, String serviceName) {
        this.consulClient = consulClient;
        this.serviceName = serviceName;
    }

    public ConsulClient getConsulClient() {
        return consulClient;
    }

    public String getServiceName() {
        return serviceName;
    }
}
