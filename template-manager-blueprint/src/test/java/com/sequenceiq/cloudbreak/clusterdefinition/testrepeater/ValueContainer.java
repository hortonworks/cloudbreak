package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

public class ValueContainer<T> {

    private T value;

    public void set(T t) {
        value = t;
    }

    public T get() {
        return value;
    }

}