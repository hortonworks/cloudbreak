package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;

public interface Mappable {

    default Integer getInt(Map<String, Object> parameters, String key) {
        String integer = getParameterOrNull(parameters, key);
        if (integer != null) {
            return Integer.parseInt(integer);
        }
        return null;
    }

    default boolean getBoolean(Map<String, Object> parameters, String key) {
        String value = getParameterOrNull(parameters, key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    Map<String, Object> asMap();

    default Map<String, Object> asSecretMap() {
        return new HashMap<>();
    }

    default void parse(Map<String, Object> parameters) {
    }

    default String getParameterOrNull(Map<String, Object> parameters, String key) {
        Object value = isNull(parameters) ? null : parameters.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
