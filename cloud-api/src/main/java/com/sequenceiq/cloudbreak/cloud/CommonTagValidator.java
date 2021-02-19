package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

public abstract class CommonTagValidator implements Validator {

    protected void validate(TagSpecification ts, Map<String, String> tags) {
        validateTagsAreNotMoreThanMaximum(ts, tags);
        validateTagsAreTooShort(tags.keySet(), ts.getMinKeyLength(), "Following tag names are too short: %s");
        validateTagAreTooLong(tags.keySet(), ts.getMaxKeyLength(), "Following tag names are too long: %s");
        validateTagsAreWellFormatted(ts.getKeyValidator(), tags.keySet(), getKeyValidator(), "Following tag names are not well formatted: %s");
        validateTagsAreTooShort(tags.values(), ts.getMinValueLength(), "Following tag values are too short: %s");
        validateTagAreTooLong(tags.values(), ts.getMaxValueLength(), "Following tag values are too long: %s");
        validateTagsAreWellFormatted(ts.getValueValidator(), tags.values(), getValueValidator(), "Following tag values are not well formatted: %s");
    }

    private void validateTagsAreNotMoreThanMaximum(TagSpecification ts, Map<String, String> tags) {
        if (tags.size() > ts.getMaxAmount()) {
            throw new IllegalArgumentException("Too much tags, maximum allowed: " + ts.getMaxAmount());
        }
    }

    private void validateTagsAreWellFormatted(String keyValidator, Collection<String> strings, Pattern keyValidator2, String s) {
        if (!keyValidator.isEmpty()) {
            Set<String> invalidKeys = strings.stream().filter(k -> !keyValidator2.matcher(k).matches()).collect(Collectors.toSet());
            if (!invalidKeys.isEmpty()) {
                throw new IllegalArgumentException(String.format(s, invalidKeys));
            }
        }
    }

    private void validateTagAreTooLong(Collection<String> strings, Integer maxKeyLength, String s) {
        Set<String> longKeys = strings.stream().filter(k -> k.length() > maxKeyLength).collect(Collectors.toSet());
        if (!longKeys.isEmpty()) {
            throw new IllegalArgumentException(String.format(s, longKeys));
        }
    }

    private void validateTagsAreTooShort(Collection<String> strings, Integer minKeyLength, String s) {
        Set<String> shortKeys = strings.stream().filter(k -> k.length() < minKeyLength).collect(Collectors.toSet());
        if (!shortKeys.isEmpty()) {
            throw new IllegalArgumentException(String.format(s, shortKeys));
        }
    }

    protected abstract Pattern getKeyValidator();

    protected abstract Pattern getValueValidator();
}
