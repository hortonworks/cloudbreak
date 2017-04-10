package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

public abstract class CommonTagValidator implements Validator {

    protected void validate(TagSpecification ts, Map<String, String> tags) {
        if (tags.size() > ts.getMaxAmount()) {
            throw new IllegalArgumentException("Too much tags, maximum allowed: " + ts.getMaxAmount());
        }
        Set<String> longKeys = tags.keySet().stream().filter(k -> k.length() > ts.getKeyLength()).collect(Collectors.toSet());
        if (!longKeys.isEmpty()) {
            throw new IllegalArgumentException(String.format("Following tag names are too long: %s", longKeys));
        }
        if (!ts.getKeyValidator().isEmpty()) {
            Set<String> invalidKeys = tags.keySet().stream().filter(k -> !getKeyValidator().matcher(k).matches()).collect(Collectors.toSet());
            if (!invalidKeys.isEmpty()) {
                throw new IllegalArgumentException(String.format("Following tag names are not well formatted: %s", invalidKeys));
            }
        }
        Set<String> longValues = tags.values().stream().filter(k -> k.length() > ts.getValueLength()).collect(Collectors.toSet());
        if (!longValues.isEmpty()) {
            throw new IllegalArgumentException(String.format("Following tag values are too long: %s", longValues));
        }
        if (!ts.getValueValidator().isEmpty()) {
            Set<String> invalidValues = tags.values().stream().filter(k -> !getValueValidator().matcher(k).matches()).collect(Collectors.toSet());
            if (!invalidValues.isEmpty()) {
                throw new IllegalArgumentException(String.format("Following tag values are not well formatted: %s", invalidValues));
            }
        }
    }

    protected abstract Pattern getKeyValidator();

    protected abstract Pattern getValueValidator();
}
