package com.sequenceiq.cloudbreak.cloud.model;

public class TagSpecification {

    private final Integer maxAmount;

    private final Integer keyLength;

    private final String keyValidator;

    private final Integer valueLength;

    private final String valueValidator;

    public TagSpecification(Integer maxAmount, Integer keyLength, String keyValidator, Integer valueLength, String valueValidator) {
        this.maxAmount = maxAmount;
        this.keyLength = keyLength;
        this.keyValidator = keyValidator;
        this.valueLength = valueLength;
        this.valueValidator = valueValidator;
    }

    public Integer getMaxAmount() {
        return maxAmount;
    }

    public Integer getKeyLength() {
        return keyLength;
    }

    public String getKeyValidator() {
        return keyValidator;
    }

    public Integer getValueLength() {
        return valueLength;
    }

    public String getValueValidator() {
        return valueValidator;
    }
}
