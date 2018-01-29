package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.Objects;
import java.util.function.Function;

public class Action<T> {
    private Function<IntegrationTestContext, T> entitySupplier;

    private Strategy strategy;

    Action(Function<IntegrationTestContext, T> entitySupplier, Strategy strategy) {
        this.entitySupplier = Objects.requireNonNull(entitySupplier);
        this.strategy = Objects.requireNonNull(strategy);
    }

    Function<IntegrationTestContext, T> getEntitySupplier() {
        return this.entitySupplier;
    }

    Strategy getStrategy() {
        return this.strategy;
    }

    <T extends Entity> T action(IntegrationTestContext integrationTestContext) throws Exception {
        T subject = (T) getEntitySupplier().apply(integrationTestContext);
        getStrategy().doAction(integrationTestContext, subject);
        return subject;
    }
}
