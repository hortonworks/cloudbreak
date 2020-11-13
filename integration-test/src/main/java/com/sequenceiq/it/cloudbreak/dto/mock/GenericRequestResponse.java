package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.Map;

public interface GenericRequestResponse<T, S> {
    T handle(S request, Map<String, String> uriParameters);
}
