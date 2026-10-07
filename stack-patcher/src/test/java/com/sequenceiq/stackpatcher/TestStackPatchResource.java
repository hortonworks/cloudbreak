package com.sequenceiq.stackpatcher;

import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;

public class TestStackPatchResource implements StackPatchResource {

    private final Long id;

    private final String resourceCrn;

    public TestStackPatchResource(Long id, String resourceCrn) {
        this.id = id;
        this.resourceCrn = resourceCrn;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }
}
