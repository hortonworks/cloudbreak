package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class TagSpecification {

    private final Integer maxAmount;

    private final Integer minKeyLength;

    private final Integer maxKeyLength;

    private final String keyValidator;

    private final Integer minValueLength;

    private final Integer maxValueLength;

    private final String valueValidator;

    public TagSpecification(Integer maxAmount, Integer minKeyLength, Integer maxKeyLength, String keyValidator, Integer minValueLength,
            Integer maxValueLength, String valueValidator) {
        this.maxAmount = maxAmount;
        this.minKeyLength = minKeyLength;
        this.maxKeyLength = maxKeyLength;
        this.keyValidator = keyValidator;
        this.maxValueLength = maxValueLength;
        this.minValueLength = minValueLength;
        this.valueValidator = valueValidator;
    }

    public Integer getMaxAmount() {
        return maxAmount;
    }

    public Integer getMaxKeyLength() {
        return maxKeyLength;
    }

    public String getKeyValidator() {
        return keyValidator;
    }

    public Integer getMaxValueLength() {
        return maxValueLength;
    }

    public String getValueValidator() {
        return valueValidator;
    }

    public Integer getMinKeyLength() {
        return minKeyLength;
    }

    public Integer getMinValueLength() {
        return minValueLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TagSpecification that = (TagSpecification) o;
        return Objects.equals(maxAmount, that.maxAmount)
                && Objects.equals(minKeyLength, that.minKeyLength)
                && Objects.equals(maxKeyLength, that.maxKeyLength)
                && Objects.equals(keyValidator, that.keyValidator)
                && Objects.equals(minValueLength, that.minValueLength)
                && Objects.equals(maxValueLength, that.maxValueLength)
                && Objects.equals(valueValidator, that.valueValidator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                maxAmount,
                minKeyLength,
                maxKeyLength,
                keyValidator,
                minValueLength,
                maxValueLength,
                valueValidator
        );
    }

    @Override
    public String toString() {
        return "TagSpecification{" +
                "maxAmount=" + maxAmount +
                ", minKeyLength=" + minKeyLength +
                ", maxKeyLength=" + maxKeyLength +
                ", keyValidator='" + keyValidator + '\'' +
                ", minValueLength=" + minValueLength +
                ", maxValueLength=" + maxValueLength +
                ", valueValidator='" + valueValidator + '\'' +
                '}';
    }
}
