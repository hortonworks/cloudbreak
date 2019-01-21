package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import static java.util.Objects.isNull;

import java.util.Collections;
import java.util.Map;

public interface Mappable {

    Mappable EMPTY = new Mappable() {
        @Override
        public Map<String, Object> asMap() {
            return Collections.emptyMap();
        }

        @Override
        public <T> T toClass(Map<String, Object> parameters) {
            return null;
        }
    };

    Map<String, Object> asMap();

    <T> T toClass(Map<String, Object> parameters);

    default String getParameterOrNull(Map<String, Object> parameters, String key) {
        Object value = isNull(parameters) ? null : parameters.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
