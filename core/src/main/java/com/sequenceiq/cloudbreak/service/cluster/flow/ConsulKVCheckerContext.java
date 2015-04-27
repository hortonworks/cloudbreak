package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ConsulKVCheckerContext extends StackContext {

    private final List<ConsulClient> consulClients;
    private final List<String> keys;
    private final String expectedValue;
    private final String failValue;

    public ConsulKVCheckerContext(Stack stack, List<ConsulClient> consulClients, List<String> keys, String expectedValue, String failValue) {
        super(stack);
        this.consulClients = consulClients;
        this.keys = keys;
        this.expectedValue = expectedValue;
        this.failValue = failValue;
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

    public String getFailValue() {
        return failValue;
    }
}
