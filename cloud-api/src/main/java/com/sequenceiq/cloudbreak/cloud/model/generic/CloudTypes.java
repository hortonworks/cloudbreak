package com.sequenceiq.cloudbreak.cloud.model.generic;

import java.util.Collection;

public abstract class CloudTypes<T> {
    private final Collection<T> types;
    private final T defaultType;

    public CloudTypes(Collection<T> types, T defaultType) {
        this.types = types;
        this.defaultType = defaultType;
    }

    public Collection<T> types() {
        return types;
    }

    public T defaultType() {
        return defaultType;
    }
}
