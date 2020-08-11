package com.sequenceiq.cloudbreak.structuredevent.controller;

import java.util.Collection;

public class CDPStructuredEventGeneralCollectionV4Response<T> {

    private Collection<T> responses;

    public CDPStructuredEventGeneralCollectionV4Response(Collection<T> responses) {
        this.responses = responses;
    }

    public Collection<T> getResponses() {
        return responses;
    }

    public void setResponses(Collection<T> responses) {
        this.responses = responses;
    }
}
