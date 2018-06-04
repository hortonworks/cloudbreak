package com.sequenceiq.cloudbreak.cloud.model;

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
}
