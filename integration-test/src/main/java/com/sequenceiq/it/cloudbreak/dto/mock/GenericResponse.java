package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

public interface GenericResponse<T> {
    T handle(DefaultModel model, Map<String, String> uriParameters);
}
