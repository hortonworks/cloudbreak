package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.Map;

public interface GenericResponse<T> {
    T handle(Map<String, String> uriParameters);
}
