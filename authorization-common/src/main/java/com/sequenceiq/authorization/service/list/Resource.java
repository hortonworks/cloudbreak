package com.sequenceiq.authorization.service.list;

import java.util.Optional;

public class Resource {

    private String resourceCrn;

    private Optional<String> parentResourceCrn;

    public Resource(String resourceCrn, Optional<String> parentResourceCrn) {
        this.resourceCrn = resourceCrn;
        this.parentResourceCrn = parentResourceCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public Optional<String> getParentResourceCrn() {
        return parentResourceCrn;
    }
}
