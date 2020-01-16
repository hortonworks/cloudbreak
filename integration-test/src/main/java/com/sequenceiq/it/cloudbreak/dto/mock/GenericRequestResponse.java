package com.sequenceiq.it.cloudbreak.dto.mock;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.mock.DefaultModel;

public interface GenericRequestResponse<T, S> {
    T handle(S request, DefaultModel model, Map<String, String> uriParameters);
}
