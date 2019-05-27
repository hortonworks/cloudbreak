package com.sequenceiq.cloudbreak.cloud.model.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic mode to hold dynamic data, any data stored in the DynamicModel must be threadsafe in that sense that multiple threads might be
 * using it, but of course it is never used concurrently. In other words if you store anything in thread local then it might not be available
 * in a subsequent calls.
 */
public class DynamicModel {

    private final Map<String, Object> parameters;

    public DynamicModel() {
        parameters = new HashMap<>();
    }

    public DynamicModel(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> clazz) {
        return (T) parameters.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(Class<T> clazz) {
        return (T) parameters.get(clazz.getName());
    }

    public String getStringParameter(String key) {
        return getParameter(key, String.class);
    }

    public void putParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public void putParameter(Class<?> clazz, Object value) {
        putParameter(clazz.getName(), value);
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
}
