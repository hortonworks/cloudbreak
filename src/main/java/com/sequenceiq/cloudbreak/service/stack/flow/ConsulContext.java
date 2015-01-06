package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ConsulContext {

    private final Stack stack;
    private final List<ConsulClient> consulClients;
    private final List<String> targets;

    public ConsulContext(Stack stack, List<ConsulClient> consulClients, List<String> targets) {
        this.stack = stack;
        this.consulClients = consulClients;
        this.targets = targets;
    }

    public Stack getStack() {
        return stack;
    }

    public List<ConsulClient> getConsulClients() {
        return consulClients;
    }

    public List<String> getTargets() {
        return targets;
    }
}
