package com.sequenceiq.cloudbreak.orchestrator.yarn.model.core;

import com.sequenceiq.common.model.JsonEntity;

public class Resource implements JsonEntity {

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
}
