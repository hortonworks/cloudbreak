package com.sequenceiq.authorization.service;

import java.util.List;
import java.util.Set;

import com.sequenceiq.authorization.resource.AuthorizationFilterableResponseCollection;

public enum ListResponseFilteringType {
    SET,
    LIST,
    FILTERABLE_RESPONSE_MODEL,
    UNSUPPORTED;

    public static ListResponseFilteringType getByClass(Class clazz) {
        if (List.class.isAssignableFrom(clazz)) {
            return LIST;
        } else if (Set.class.isAssignableFrom(clazz)) {
            return SET;
        } else if (AuthorizationFilterableResponseCollection.class.isAssignableFrom(clazz)) {
            return FILTERABLE_RESPONSE_MODEL;
        }
        return UNSUPPORTED;
    }
}
