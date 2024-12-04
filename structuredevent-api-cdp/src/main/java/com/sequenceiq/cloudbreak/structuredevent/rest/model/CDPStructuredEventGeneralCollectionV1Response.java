package com.sequenceiq.cloudbreak.structuredevent.rest.model;

import java.util.Collection;

import io.swagger.v3.oas.annotations.media.Schema;

public class CDPStructuredEventGeneralCollectionV1Response<T> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Collection<T> responses;

    public CDPStructuredEventGeneralCollectionV1Response(Collection<T> responses) {
        this.responses = responses;
    }

    public Collection<T> getResponses() {
        return responses;
    }

    public void setResponses(Collection<T> responses) {
        this.responses = responses;
    }
}
