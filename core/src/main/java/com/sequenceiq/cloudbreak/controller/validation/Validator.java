package com.sequenceiq.cloudbreak.controller.validation;

public interface Validator<T> {
    ValidationResult validate(T subject);
}
