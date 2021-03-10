package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import java.io.Serializable;

public class Resource implements Serializable {

    private int cpus;

    private int memory;

    public int getCpus() {
        return cpus;
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    @Override
    public String toString() {
        return "Resource {"
                + "cpus=" + cpus
                + ", memory=" + memory
                + '}';
    }
}
