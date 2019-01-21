package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.Collections;
import java.util.Map;

public interface Mappable {

    Map<String, Object> asMap();

    <T> T toClass(Map<String, Object> parameters);

    default String getParameterOrNull(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

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
}
