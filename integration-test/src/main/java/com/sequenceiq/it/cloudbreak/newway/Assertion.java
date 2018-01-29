package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Assertion<T> {
    private Function<IntegrationTestContext, T> entitySupplier;

    private BiConsumer<T, IntegrationTestContext> check;

    Assertion(Function<IntegrationTestContext, T> entitySupplier, BiConsumer<T, IntegrationTestContext> check) {
        this.entitySupplier = Objects.requireNonNull(entitySupplier);
        this.check = Objects.requireNonNull(check);
    }

    public BiConsumer<T, IntegrationTestContext> getCheck() {
        return check;
    }

    public Function<IntegrationTestContext, T> getEntitySupplier() {
        return entitySupplier;
    }

    void doAssertion(IntegrationTestContext integrationTestContext) {
        T subject = (T) getEntitySupplier().apply(integrationTestContext);
        getCheck().accept(subject, integrationTestContext);
    }
}
