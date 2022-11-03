package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class Cloud {

    private String name;

    public Cloud() {
    }

    public Cloud(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
