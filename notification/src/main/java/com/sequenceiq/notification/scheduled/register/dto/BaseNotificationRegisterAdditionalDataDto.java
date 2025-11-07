package com.sequenceiq.notification.scheduled.register.dto;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class BaseNotificationRegisterAdditionalDataDto {

    private final String name;

    private final String crn;

    protected BaseNotificationRegisterAdditionalDataDto(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseNotificationRegisterAdditionalDataDto that = (BaseNotificationRegisterAdditionalDataDto) o;
        return Objects.equals(name, that.name) && Objects.equals(crn, that.crn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, crn);
    }

    @Override
    public String toString() {
        return "BasedNotificationRegisterDto{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                '}';
    }

    protected String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = Arrays.stream(input.toLowerCase().split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1) + " ")
                .collect(Collectors.joining());
        return result.trim();
    }
}
