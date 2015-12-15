package com.sequenceiq.cloudbreak.shell.completion;

public abstract class AbstractCompletion {

    private final String name;

    protected AbstractCompletion(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
