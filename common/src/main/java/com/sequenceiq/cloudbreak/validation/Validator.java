package com.sequenceiq.cloudbreak.validation;

public interface Validator<T> {
    ValidationResult validate(T subject);
}
