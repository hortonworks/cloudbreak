package com.sequenceiq.cloudbreak.cloud.model.generic;

import java.util.HashMap;
import java.util.Map;

public class DynamicModel {

    private final Map<String, Object> parameters;

    public DynamicModel() {
        parameters = new HashMap<>();
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

    public void putParameter(Class clazz, Object value) {
        putParameter(clazz.getName(), value);
    }

    public void putAll(Map<String, Object> params) {
        parameters.putAll(params);
    }
}
