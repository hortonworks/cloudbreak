package com.sequenceiq.notification.scheduled.register.dto;

import java.util.List;
import java.util.Objects;

public class BaseNotificationRegisterAdditionalDataDtos<E extends BaseNotificationRegisterAdditionalDataDto> {

    private List<E> results;

    public BaseNotificationRegisterAdditionalDataDtos(List<E> results) {
        this.results = results;
    }

    public List<E> getResults() {
        return results;
    }

    public void setResults(List<E> results) {
        this.results = results;
    }

    public static <E extends BaseNotificationRegisterAdditionalDataDto> Builder<E> builder() {
        return new Builder<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseNotificationRegisterAdditionalDataDtos<?> that = (BaseNotificationRegisterAdditionalDataDtos<?>) o;
        return Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(results);
    }

    @Override
    public String toString() {
        return "BasedNotificationRegisterDtos{" +
                "results=" + results +
                '}';
    }

    public static class Builder<E extends BaseNotificationRegisterAdditionalDataDto> {

        private List<E> results;

        public Builder<E> results(List<E> results) {
            this.results = results;
            return this;
        }

        public BaseNotificationRegisterAdditionalDataDtos<E> build() {
            return new BaseNotificationRegisterAdditionalDataDtos<>(this.results);
        }
    }
}
