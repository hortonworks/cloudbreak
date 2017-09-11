package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Date;

public class ContainerName {

    private final String namePrefix;

    private final String name;

    public ContainerName(String name, String namePrefix) {
        this.namePrefix = namePrefix;
        this.name = name;
    }

    public String getName() {
        return (name != null) ? name :  String.format("%s-%s", namePrefix, String.valueOf(new Date().getTime()));
    }
}
