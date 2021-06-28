package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.domain.projection.StackIdView;

class StackIdViewImpl implements StackIdView {

    private final Long id;

    private final String name;

    private final String crn;

    StackIdViewImpl(Long id, String name, String crn) {
        this.id = id;
        this.name = name;
        this.crn = crn;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCrn() {
        return crn;
    }
}
