package com.sequenceiq.authorization.service.list;

import java.util.Objects;
import java.util.Optional;

public class ResourceWithId extends Resource {

    private final long id;

    public ResourceWithId(
            long id,
            String resourceCrn) {
        super(Objects.requireNonNull(resourceCrn), Optional.empty());
        this.id = id;
    }

    public ResourceWithId(
            long id,
            String resourceCrn,
            String parentResourceCrn) {
        super(Objects.requireNonNull(resourceCrn), Optional.ofNullable(parentResourceCrn));
        this.id = id;
    }

    public ResourceWithId(
            long id,
            String resourceCrn,
            Optional<String> parentResourceCrn) {
        super(Objects.requireNonNull(resourceCrn), Objects.requireNonNull(parentResourceCrn));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
