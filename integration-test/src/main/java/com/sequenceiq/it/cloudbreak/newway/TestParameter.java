package com.sequenceiq.it.cloudbreak.newway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestParameter.class);

    private static final String REQUIRED_KEY_PREFIX = "NN_";

    private final Map<String, String> parameters;

    TestParameter() {
        parameters = new HashMap<>();
    }

    // TODO: 2018. 06. 22. optimalize
    public String get(String key) {
        Optional<String> valueAsProperty = Optional.ofNullable(parameters.get(key));
        if (!valueAsProperty.isPresent()) {
            LOGGER.info("key has not been found as property, trying as environment variable");
            valueAsProperty = Optional.ofNullable(parameters.get(key.toUpperCase().replaceAll("\\.", "_")));
        }
        LOGGER.info(valueAsProperty.isPresent()
                ? String.format("Aquiring key %s resulting: %s", key, valueAsProperty.get())
                : String.format("Aquiring key %s, but no result has found.", key));

        if (key.startsWith(REQUIRED_KEY_PREFIX) && !valueAsProperty.isPresent()) {
            throw new MissingExpectedParameterException(key);
        }
        return valueAsProperty.isPresent() ? valueAsProperty.get() : null;
    }

    public String getWithDefault(String key, String defaultValue) {
        Optional<String> value = Optional.ofNullable(get(key));
        return value.orElse(defaultValue);
    }

    public void put(String key, String value) {
        parameters.put(key, value);
    }

    public void putAll(Map<String, String> all) {
        parameters.putAll(all);
    }

    public int size() {
        return parameters.size();
    }
}
