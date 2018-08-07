package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public abstract class Entity {
    private final String entityId;

    private Strategy creationStrategy;

    protected Entity(String id) {
        entityId = id;
    }

    String getEntityId() {
        return entityId;
    }

    protected void setCreationStrategy(Strategy strategy) {
        creationStrategy = strategy;
    }

    void create(IntegrationTestContext integrationTestContext) throws Exception {
        if (creationStrategy != null) {
            creationStrategy.doAction(integrationTestContext, this);
        }
    }
}
