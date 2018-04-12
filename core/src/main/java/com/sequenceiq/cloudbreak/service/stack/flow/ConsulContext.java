package com.sequenceiq.cloudbreak.service.stack.flow;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import java.util.List;

public class ConsulContext extends StackContext {

    private final ConsulClient consulClient;

    private final List<String> targets;

    public ConsulContext(Stack stack, ConsulClient consulClient, List<String> targets) {
        super(stack);
        this.consulClient = consulClient;
        this.targets = targets;
    }

    public ConsulClient getConsulClient() {
        return consulClient;
    }

    public List<String> getTargets() {
        return targets;
    }
}
