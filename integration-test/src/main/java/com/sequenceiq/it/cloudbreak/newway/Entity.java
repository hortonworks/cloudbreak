package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public abstract class Entity {
    private String entityId;

    private Strategy creationStrategy;

    public Entity(String id) {
        this.entityId = id;
    }

    String getEntityId() {
        return entityId;
    }

    protected void setCreationStrategy(Strategy strategy) {
        this.creationStrategy = strategy;
    }

    void create(IntegrationTestContext integrationTestContext) throws Exception {
        if (creationStrategy != null) {
            creationStrategy.doAction(integrationTestContext, this);
        }
    }
}
