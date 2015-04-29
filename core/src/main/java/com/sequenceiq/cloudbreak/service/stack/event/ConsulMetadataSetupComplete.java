package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.Stack;

public class ConsulMetadataSetupComplete {

    private Stack stack;
    private String ambariIp;

    public ConsulMetadataSetupComplete(Stack stack, String ambariIp) {
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
