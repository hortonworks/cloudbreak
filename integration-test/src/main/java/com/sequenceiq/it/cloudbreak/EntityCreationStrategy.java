package com.sequenceiq.it.cloudbreak;

public class EntityCreationStrategy<T extends Entity> {
    public  T setCreationStrategy(T entity, Strategy strategy) {
        entity.setCreationStrategy(strategy);
        return entity;
    }
}
