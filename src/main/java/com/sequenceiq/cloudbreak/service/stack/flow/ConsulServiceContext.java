package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ConsulServiceContext {

    private final Stack stack;
    private final List<ConsulClient> consulClients;
    private final String serviceName;

    public ConsulServiceContext(Stack stack, List<ConsulClient> consulClients, String serviceName) {
        this.stack = stack;
        this.consulClients = consulClients;
        this.serviceName = serviceName;
    }

    public Stack getStack() {
        return stack;
    }

    public List<ConsulClient> getConsulClients() {
        return consulClients;
    }

    public String getServiceName() {
        return serviceName;
    }
}
