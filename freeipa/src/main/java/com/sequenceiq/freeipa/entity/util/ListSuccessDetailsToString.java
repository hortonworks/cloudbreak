package com.sequenceiq.freeipa.entity.util;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;

public class ListSuccessDetailsToString extends ListToString<SuccessDetails> {
    private static final TypeReference<List<SuccessDetails>> TYPE_REFERENCE = new TypeReference<>() { };

    public TypeReference<List<SuccessDetails>> getTypeReference() {
        return TYPE_REFERENCE;
    }
}
