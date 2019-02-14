package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import org.junit.runners.model.Statement;

class RepeatedStatement<T> extends Statement {

    private final Statement test;

    private final Iterable<T> values;

    private final AccessibleErrorCollector errorCollector;

    RepeatedStatement(Statement test, Iterable<T> values,
            AccessibleErrorCollector errorCollector) {
        this.test = test;
        this.values = values;
        this.errorCollector = errorCollector;
    }

    @Override
    public void evaluate() throws Throwable {
        for (T value : values) {
            try {
                test.evaluate();
            } catch (Throwable t) {
                errorCollector.addError(new AssertionError(String.format("For value: %s", value.toString()), t));
            }
        }
        errorCollector.verify();
    }
}
