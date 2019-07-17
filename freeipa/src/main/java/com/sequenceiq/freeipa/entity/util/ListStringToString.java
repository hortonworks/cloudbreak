package com.sequenceiq.freeipa.entity.util;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

public class ListStringToString extends ListToString<String> {
    private static final TypeReference<List<String>> TYPE_REFERENCE = new TypeReference<>() { };

    public TypeReference<List<String>> getTypeReference() {
        return TYPE_REFERENCE;
    }
}
