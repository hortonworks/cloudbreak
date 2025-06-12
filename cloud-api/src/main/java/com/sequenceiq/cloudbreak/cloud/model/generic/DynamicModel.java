package com.sequenceiq.cloudbreak.cloud.model.generic;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

/**
 * Generic model to hold dynamic data. Any data stored in the DynamicModel must be thread safe in the sense that multiple threads might be
 * using it, but of course it is never used concurrently. In other words, if you store anything in thread local then it might not be available
 * in subsequent calls.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicModel.class);

    private final Map<String, Object> parameters;

    /**
     * Constructs a new {@link DynamicModel} with an empty initial map. The enclosed map is always mutable, so it is possible to add parameters later using
     * {@link #putParameter(String, Object)} or {@link #putParameter(Class, Object)}.
     */
    public DynamicModel() {
        parameters = new HashMap<>();
    }

    /**
     * Constructs a new {@link DynamicModel} with the initial map containing the entries of the supplied {@code parameters}. The enclosed map is always
     * mutable (even if {@code parameters} is unmodifiable), so it is possible to add further parameters later using {@link #putParameter(String, Object)} or
     * {@link #putParameter(Class, Object)}. Passing in {@code null} will be silently accepted and treated as if an empty map had been specified.
     * @param parameters initial mappings; may be {@code null} (treated like an empty map); may be empty
     */
    public DynamicModel(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new HashMap<>() : new HashMap<>(parameters);
    }

    /**
     * Retrieves a parameter with key {@code key} and expected type represented by {@code clazz}.
     * @param key parameter key
     * @param clazz {@code Class} instance representing the expected type of the parameter value; must not be {@code null}
     * @param <T> expected type of the parameter value
     * @return parameter value, or {@code null} if the requested parameter is absent
     * @throws NullPointerException if {@code clazz == null}
     */
    public <T> T getParameter(String key, Class<T> clazz) {
        try {
            return clazz.cast(parameters.get(key));
        } catch (ClassCastException e) {
            LOGGER.error("Can't cast to {}, trying to read it as an Object, then write it to json and try to read it to {}", clazz, clazz);
            Object object = parameters.get(key);
            String objectAsJson = JsonUtil.writeValueAsStringSilent(object);
            try {
                return JsonUtil.readValue(objectAsJson, clazz);
            } catch (IOException ex) {
                LOGGER.info("Can't read json as class: {}", clazz, ex);
                throw new CloudbreakServiceException(ex);
            }
        }
    }

    /**
     * Retrieves a parameter with key {@code clazz.getName()} and expected type represented by {@code clazz}.
     * @param clazz {@code Class} instance representing the expected type of the parameter value, and also determining the parameter key; must not be
     *          {@code null}
     * @param <T> expected type of the parameter value
     * @return parameter value, or {@code null} if the requested parameter is absent
     * @throws NullPointerException if {@code clazz == null}
     * @throws ClassCastException if the requested parameter exists but its value has a type incompatible with {@code T}
     */
    public <T> T getParameter(Class<T> clazz) {
        return getParameter(clazz.getName(), clazz);
    }

    /**
     * Retrieves a parameter with key {@code key} and expected type {@code String}.
     * @param key parameter key
     * @return parameter value, or {@code null} if the requested parameter is absent
     * @throws ClassCastException if the requested parameter exists but its value has a type different from {@code String}
     */
    public String getStringParameter(String key) {
        return getParameter(key, String.class);
    }

    /**
     * Adds a parameter with key {@code key} and value {@code value}. If a parameter with such a key already exists, its old value will be replaced with
     * {@code value}.
     * @param key parameter key
     * @param value parameter value; may be {@code null}
     */
    public void putParameter(String key, Object value) {
        parameters.put(key, value);
    }

    /**
     * Adds a parameter with key {@code clazz.getName()} and value {@code value}. The actual type of {@code value} is not checked with respect to {@code clazz}.
     * If a parameter with such a key already exists, its old value will be replaced with {@code value}.
     * @param clazz {@code Class} instance determining the parameter key; must not be {@code null}
     * @param value parameter value; may be {@code null}
     * @throws NullPointerException if {@code clazz == null}
     */
    public void putParameter(Class<?> clazz, Object value) {
        putParameter(clazz.getName(), value);
    }

    /**
     * Retrieves an immutable view of the parameter map enclosed by {@code this}. The result is a new object wrapping the internal parameter map so that it
     * provides an immutable but live view (as opposed to being a simple "snapshot copy"), so adding new parameters to or replacing existing parameter values in
     * {@code this} will be also reflected in the returned object.
     * @return new unmodifiable view of the parameter map; never {@code null}; may be empty
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Checks if a parameter with key {@code key} exists.
     * @param key parameter key
     * @return {@code true} if a parameter with key {@code key} exists; {@code false} otherwise
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public String toString() {
        return "DynamicModel{" +
                "parameters=" + parameters +
                '}';
    }

}
