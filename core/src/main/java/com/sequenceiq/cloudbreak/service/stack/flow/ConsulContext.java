package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

public class ConsulContext extends StackDependentPollerObject {

    private final List<ConsulClient> consulClients;
    private final List<String> targets;

    public ConsulContext(Stack stack, List<ConsulClient> consulClients, List<String> targets) {
        super(stack);
        this.consulClients = consulClients;
        this.targets = targets;
    }

    public List<ConsulClient> getConsulClients() {
        return consulClients;
    }

    public List<String> getTargets() {
        return targets;
    }
}
