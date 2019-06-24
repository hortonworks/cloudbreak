package com.sequenceiq.freeipa.dto;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;

public class StackIdWithStatus {

    private final Long id;

    private final Status status;

    public StackIdWithStatus(Long id, Status status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }
}
