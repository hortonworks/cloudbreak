package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;
import java.util.Optional;

public class StackParamValidation {

    private final String paramName;

    private final Class<?> clazz;

    private final boolean required;

    private final Optional<String> regex;

    public StackParamValidation(String paramName, Boolean required, Class<?> clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.required = required;
        this.clazz = clazz;
        this.regex = regex;
    }

    public String getName() {
        return paramName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Boolean getRequired() {
        return required;
    }

    public Optional<String> getRegex() {
        return regex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackParamValidation that = (StackParamValidation) o;
        return required == that.required &&
                Objects.equals(paramName, that.paramName) &&
                Objects.equals(clazz, that.clazz) &&
                Objects.equals(regex, that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paramName, clazz, required, regex);
    }
}
