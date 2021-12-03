package com.sequenceiq.cloudbreak.validation;

import static com.sequenceiq.cloudbreak.validation.ValidationResult.State.ERROR;
import static com.sequenceiq.cloudbreak.validation.ValidationResult.State.VALID;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

public class ValidationResult {

    private final State state;

    private final List<String> errors;

    private final List<String> warnings;

    private String formattedErrors = "";

    private String formattedWarnings = "";

    private final String prefix;

    private ValidationResult(State state, SortedSet<String> errors, SortedSet<String> warnings, String prefix) {
        this.state = state;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.prefix = prefix;
        if (!StringUtils.isEmpty(prefix)) {
            formattedErrors = prefix + ": \n";
            formattedWarnings = prefix + ": \n";
        }
        formattedErrors = formatNumberedList(this.errors);
        formattedWarnings = formatNumberedList(this.warnings);
    }

    private String formatNumberedList(List<String> issues) {
        return IntStream.range(0, issues.size())
                .mapToObj(i -> getNumberingIfRequired(issues.size(), i) + issues.get(i))
                .collect(Collectors.joining("\n"));
    }

    private String getNumberingIfRequired(int errorSize, int i) {
        return errorSize == 1 ? "" : i + 1 + ". ";
    }

    public ValidationResult merge(ValidationResult other) {
        State mergeState = state == ERROR || other.state == ERROR ? ERROR : VALID;
        SortedSet<String> mergedError = new TreeSet<>(this.errors);
        mergedError.addAll(other.errors);
        SortedSet<String> mergedWarning = new TreeSet<>(this.warnings);
        mergedError.addAll(other.warnings);
        return new ValidationResult(mergeState, mergedError, mergedWarning, other.prefix);
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

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public String getFormattedWarnings() {
        return formattedWarnings;
    }

    public boolean hasWarning() {
        return !warnings.isEmpty();
    }

    public enum State {
        VALID, ERROR
    }

    public static ValidationResult empty() {
        return builder().build();
    }

    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {

        private State state = VALID;

        private final SortedSet<String> errors = new TreeSet<>();

        private final SortedSet<String> warnings = new TreeSet<>();

        private String prefix;

        public ValidationResultBuilder error(String error) {
            if (state == VALID) {
                state = ERROR;
            }
            errors.add(error);
            return this;
        }

        public ValidationResultBuilder warning(String warning) {
            warnings.add(warning);
            return this;
        }

        public ValidationResultBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ValidationResultBuilder merge(ValidationResult other) {
            if (other != null) {
                other.getErrors().forEach(this::error);
                other.getWarnings().forEach(this::warning);
            }
            return this;
        }

        public ValidationResultBuilder ifError(Supplier<Boolean> validationFunction, String errorMessage) {
            if (validationFunction.get()) {
                error(errorMessage);
            }
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(state, errors, warnings, prefix);
        }
    }
}
