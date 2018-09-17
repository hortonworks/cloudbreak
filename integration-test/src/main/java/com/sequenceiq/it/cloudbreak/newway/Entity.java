package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public abstract class Entity {

    private String entityId;

    private Strategy creationStrategy;

    protected Entity(String entityId) {
        this.entityId = entityId;
    }

    protected Entity() {
        entityId = getClass().getSimpleName().toUpperCase();
    }

    String getEntityId() {
        return entityId;
    }

    public void setCreationStrategy(Strategy strategy) {
        creationStrategy = strategy;
    }

    public void create(IntegrationTestContext integrationTestContext) throws Exception {
        if (creationStrategy != null) {
            creationStrategy.doAction(integrationTestContext, this);
        }
    }
}
