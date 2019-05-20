package com.sequenceiq.it.cloudbreak;

import java.util.Objects;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class ResourceAction<T> {
    private final Function<IntegrationTestContext, T> entitySupplier;

    private final Strategy strategy;

    public ResourceAction(Function<IntegrationTestContext, T> entitySupplier, Strategy strategy) {
        this.entitySupplier = Objects.requireNonNull(entitySupplier);
        this.strategy = Objects.requireNonNull(strategy);
    }

    Function<IntegrationTestContext, T> getEntitySupplier() {
        return entitySupplier;
    }

    Strategy getStrategy() {
        return strategy;
    }

    <T extends Entity> T action(IntegrationTestContext integrationTestContext) throws Exception {
        T subject = ((Function<IntegrationTestContext, T>) entitySupplier).apply(integrationTestContext);
        strategy.doAction(integrationTestContext, subject);
        return subject;
    }
}
