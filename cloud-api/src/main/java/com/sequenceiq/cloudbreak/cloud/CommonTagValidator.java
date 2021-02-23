package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

public abstract class CommonTagValidator implements Validator, TagValidator {

    public abstract TagSpecification getTagSpecification();

    protected String transform(String tag) {
        return tag;
    }

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        ValidationResult validationResult = validateTags(getTagSpecification(), cloudStack.getTags());
        if (validationResult.hasError()) {
            throw new IllegalArgumentException(validationResult.getFormattedErrors());
        }
    }

    @Override
    public ValidationResult validateTags(Map<String, String> tags) {
        return validateTags(getTagSpecification(), tags);
    }

    protected ValidationResult validateTags(TagSpecification ts, Map<String, String> tags) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        validateTagsAreNotMoreThanMaximum(ts, tags, validationResultBuilder);
        validateTagsAreTooShort(tags.keySet(), ts.getMinKeyLength(), "Following tag names are too short: %s", validationResultBuilder);
        validateTagAreTooLong(tags.keySet(), ts.getMaxKeyLength(), "Following tag names are too long: %s", validationResultBuilder);
        validateTagsAreWellFormatted(ts.getKeyValidator(), tags.keySet(), getKeyValidator(),
                "Following tag names are not well formatted: %s. Key of the tag should match for %s regular expression", validationResultBuilder);
        validateTagsAreTooShort(tags.values(), ts.getMinValueLength(), "Following tag values are too short: %s", validationResultBuilder);
        validateTagAreTooLong(tags.values(), ts.getMaxValueLength(), "Following tag values are too long: %s", validationResultBuilder);
        validateTagsAreWellFormatted(ts.getValueValidator(), tags.values(), getValueValidator(),
                "Following tag values are not well formatted: %s. Value of the tag should match for %s regular expression", validationResultBuilder);
        return validationResultBuilder.build();
    }

    private void validateTagsAreNotMoreThanMaximum(TagSpecification ts, Map<String, String> tags, ValidationResultBuilder validationResultBuilder) {
        if (tags.size() > ts.getMaxAmount()) {
            validationResultBuilder.error("Too much tags, maximum allowed: " + ts.getMaxAmount());
        }
    }

    private void validateTagsAreWellFormatted(String keyValidator, Collection<String> strings, Pattern keyValidator2, String s,
            ValidationResultBuilder validationResultBuilder) {
        if (!keyValidator.isEmpty()) {
            Set<String> invalidKeys = strings.stream().filter(k -> !keyValidator2.matcher(transform(k)).matches()).collect(Collectors.toSet());
            if (!invalidKeys.isEmpty()) {
                validationResultBuilder.error(String.format(s, invalidKeys, keyValidator2.pattern()));
            }
        }
    }

    private void validateTagAreTooLong(Collection<String> strings, Integer maxKeyLength, String s, ValidationResultBuilder validationResultBuilder) {
        Set<String> longKeys = strings.stream().filter(k -> k.length() > maxKeyLength).collect(Collectors.toSet());
        if (!longKeys.isEmpty()) {
            validationResultBuilder.error(String.format(s, longKeys));
        }
    }

    private void validateTagsAreTooShort(Collection<String> strings, Integer minKeyLength, String s, ValidationResultBuilder validationResultBuilder) {
        Set<String> shortKeys = strings.stream().filter(k -> k.length() < minKeyLength).collect(Collectors.toSet());
        if (!shortKeys.isEmpty()) {
            validationResultBuilder.error(String.format(s, shortKeys));
        }
    }

    protected abstract Pattern getKeyValidator();

    protected abstract Pattern getValueValidator();
}
