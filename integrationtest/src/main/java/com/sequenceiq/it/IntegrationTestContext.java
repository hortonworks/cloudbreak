package com.sequenceiq.it;

import java.util.HashMap;
import java.util.Map;

public class IntegrationTestContext {
    public static final String AUTH_TOKEN = "AUTH_TOKEN";

    private Map<String, Object> contextParameters = new HashMap<>();
    private Map<String, Object> cleanUpParameters = new HashMap<>();

    public IntegrationTestContext() {
    }

    public IntegrationTestContext(Map<String, Object> contextParameters) {
        this.contextParameters = contextParameters;
    }

    public String getContextParam(String paramKey) {
        return getContextParam(paramKey, String.class);
    }

    public <T> T getContextParam(String paramKey, Class<T> clazz) {
        Object val = contextParameters.get(paramKey);
        if (val == null || clazz.isInstance(val)) {
            return clazz.cast(val);
        } else {
            throw new IllegalArgumentException("Param value is not type of " + clazz);
        }
    }

    public void putContextParam(String paramKey, Object paramValue) {
        putContextParam(paramKey, paramValue, false);
    }

    public void putContextParam(String paramKey, Object paramValue, boolean cleanUp) {
        contextParameters.put(paramKey, paramValue);
        if (cleanUp) {
            putCleanUpParam(paramKey, paramValue);
        }
    }

    public void putCleanUpParam(String paramKey, Object paramValue) {
        cleanUpParameters.put(paramKey, paramValue);
    }

    public String getCleanUpParameter(String key) {
        return getCleanUpParameter(key, String.class);
    }

    public <T> T getCleanUpParameter(String key, Class<T> clazz) {
        Object val = cleanUpParameters.get(key);
        if (val == null || clazz.isInstance(val)) {
            return clazz.cast(val);
        } else {
            throw new IllegalArgumentException("Param value is not type of " + clazz);
        }
    }
}
