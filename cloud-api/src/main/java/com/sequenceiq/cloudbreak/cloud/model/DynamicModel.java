package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class DynamicModel {

    private Map<String, Object> parameters;

    public DynamicModel() {
        parameters = new HashMap<>();
    }

    public <T> T getParameter(String key, Class<T> clazz) {
        return (T) parameters.get(key);
    }

    public <T> T getParameter(Class<T> clazz) {
        return (T) parameters.get(clazz.getName());
    }

    public void putParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public void putParameter(Class clazz, Object value) {
        putParameter(clazz.getName(), value);
    }
}
