package com.sequenceiq.authorization.service.list;

import java.util.Objects;
import java.util.Optional;

public class AuthorizationResource {

    private final long id;

    private final String resourceCrn;

    private final Optional<String> parentResourceCrn;

    public AuthorizationResource(
            long id,
            String resourceCrn) {
        this.id = id;
        this.resourceCrn = Objects.requireNonNull(resourceCrn);
        parentResourceCrn = Optional.empty();
    }

    public AuthorizationResource(
            long id,
            String resourceCrn,
            String parentResourceCrn) {
        this.id = id;
        this.resourceCrn = Objects.requireNonNull(resourceCrn);
        this.parentResourceCrn = Optional.ofNullable(parentResourceCrn);
    }

    public AuthorizationResource(
            long id,
            String resourceCrn,
            Optional<String> parentResourceCrn) {
        this.id = id;
        this.resourceCrn = Objects.requireNonNull(resourceCrn);
        this.parentResourceCrn = Objects.requireNonNull(parentResourceCrn);
    }

    public long getId() {
        return id;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public Optional<String> getParentResourceCrn() {
        return parentResourceCrn;
    }
}
