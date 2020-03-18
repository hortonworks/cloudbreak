package com.sequenceiq.authorization.resource;

import java.util.Collection;

public interface AuthorizationFilterableResponseCollection<T extends ResourceCrnAwareApiModel> {
    Collection<T> getResponses();

    void setResponses(Collection<T> filtered);
}
