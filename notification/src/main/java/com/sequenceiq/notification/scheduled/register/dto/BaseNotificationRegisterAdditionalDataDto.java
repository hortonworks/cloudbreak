package com.sequenceiq.notification.scheduled.register.dto;

import java.util.Objects;

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
}
