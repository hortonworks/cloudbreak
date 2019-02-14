package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import java.util.List;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ListGenerator<T> implements Generator<T> {

    private final ValueContainer<T> currentValue = new ValueContainer<>();

    private final AccessibleErrorCollector errorCollector = new AccessibleErrorCollector();

    private final List<T> values;

    public ListGenerator(List<T> values) {
        this.values = values;
    }

    @Override
    public T value() {
        return currentValue.get();
    }

    @Override
    public Statement apply(Statement test, Description description) {
        return new RepeatedStatement<>(test, new SyncingIterable<>(values,
                currentValue), errorCollector);
    }
}