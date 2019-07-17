package com.sequenceiq.freeipa.entity.util;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;

public class ListFailureDetailsToString extends ListToString<FailureDetails> {
    private static final TypeReference<List<FailureDetails>> TYPE_REFERENCE = new TypeReference<>() { };

    public TypeReference<List<FailureDetails>> getTypeReference() {
        return TYPE_REFERENCE;
    }
}
