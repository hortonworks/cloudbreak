package com.sequenceiq.cloudbreak.orchestrator.model;

public class OrchestratorContext {

    private final Long id;
    private final String name;
    private final String platform;
    private final String owner;

    public OrchestratorContext(Long id, String name, String platform, String owner) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatform() {
        return platform;
    }

    public String getOwner() {
        return owner;
    }
}
