package com.sequenceiq.flow.core;

import java.util.Map;

public interface EventParameterFactory {
    Map<String, Object> createEventParameters(Long resourceId);
}
