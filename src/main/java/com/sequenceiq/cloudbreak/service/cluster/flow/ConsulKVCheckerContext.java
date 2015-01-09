package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ConsulKVCheckerContext {

    private final Stack stack;
    private final List<ConsulClient> consulClients;
    private final List<String> keys;
    private final String expectedValue;

    public ConsulKVCheckerContext(Stack stack, List<ConsulClient> consulClients, List<String> keys, String expectedValue) {
        this.stack = stack;
        this.consulClients = consulClients;
        this.keys = keys;
        this.expectedValue = expectedValue;
    }

    public Stack getStack() {
        return stack;
    }

    public List<ConsulClient> getConsulClients() {
        return consulClients;
    }

    public List<String> getKeys() {
        return keys;
    }

    public String getExpectedValue() {
        return expectedValue;
    }
}
