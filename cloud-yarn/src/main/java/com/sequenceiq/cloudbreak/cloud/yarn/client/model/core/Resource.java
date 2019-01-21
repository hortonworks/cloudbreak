package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

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
