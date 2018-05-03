package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class Os {

    private String name;

    public Os() {
    }

    public Os(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
