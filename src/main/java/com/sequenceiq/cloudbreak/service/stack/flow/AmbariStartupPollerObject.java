package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.domain.Stack;

public class AmbariStartupPollerObject {

    private Stack stack;
    private String ambariIp;

    public AmbariStartupPollerObject(Stack stack, String ambariIp) {
        this.stack = stack;
        this.ambariIp = ambariIp;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }
}
