package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.util.ValidationResult.State.ERROR;
import static com.sequenceiq.cloudbreak.util.ValidationResult.State.VALID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValidationResult {

    private final State state;

    private final List<String> errors;

    private final String formattedErrors;

    private ValidationResult(State state, List<String> errors) {
        this.state = state;
        this.errors = errors;
        formattedErrors = IntStream.range(0, errors.size())
                .mapToObj(i -> i + 1 + ". " + errors.get(i))
                .collect(Collectors.joining("\n "));
    }

    public ValidationResult merge(ValidationResult other) {
        State mergeState = state == ERROR || other.state == ERROR ? ERROR : VALID;
        List<String> mergedErrors = new ArrayList<>(errors);
        mergedErrors.addAll(other.errors);
        return new ValidationResult(mergeState, mergedErrors);
    }

    public State getState() {
        return state;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public String getFormattedErrors() {
        return formattedErrors;
    }

    public boolean hasError() {
        return state == ERROR;
    }

    public enum State {
        VALID, ERROR
    }

    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {

        private State state = VALID;

        private final List<String> errors = new ArrayList<>();

        public ValidationResultBuilder error(String error) {
            if (state == VALID) {
                state = ERROR;
            }
            errors.add(error);
            return this;
        }

        public ValidationResultBuilder merge(ValidationResult other) {
            other.getErrors().forEach(this::error);
            return this;
        }

        public ValidationResultBuilder ifError(Supplier<Boolean> validationFunction, String errorMessage) {
            if (validationFunction.get()) {
                error(errorMessage);
            }
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(state, errors);
        }
    }
}
