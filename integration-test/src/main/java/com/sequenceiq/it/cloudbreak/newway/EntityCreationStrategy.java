package com.sequenceiq.it.cloudbreak.newway;

public class EntityCreationStrategy<T extends Entity> {
    public  T setCreationStrategy(T entity, Strategy strategy) {
        entity.setCreationStrategy(strategy);
        return entity;
    }
}
