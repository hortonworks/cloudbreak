package com.sequenceiq.it.cloudbreak;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class Assertion<T> {
    private final Function<IntegrationTestContext, T> entitySupplier;

    private final BiConsumer<T, IntegrationTestContext> check;

    public Assertion(Function<IntegrationTestContext, T> entitySupplier, BiConsumer<T, IntegrationTestContext> check) {
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
        T subject = entitySupplier.apply(integrationTestContext);
        check.accept(subject, integrationTestContext);
    }
}
