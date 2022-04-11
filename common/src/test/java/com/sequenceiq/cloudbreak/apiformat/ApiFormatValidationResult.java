package com.sequenceiq.cloudbreak.apiformat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class ApiFormatValidationResult {

    private Class<?> validatedClass;

    private List<String> errors;

    ApiFormatValidationResult(Class<?> validatedClass) {
        this.validatedClass = validatedClass;
        errors = new ArrayList<>();
    }

    public Class<?> getValidatedClass() {
        return validatedClass;
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(validatedClass.getName());
        if (hasError()) {
            result.append(System.lineSeparator());
            result.append(errors.stream().map(e -> "- " + e).collect(Collectors.joining(System.lineSeparator())));
        }
        return result.toString();
    }
}
