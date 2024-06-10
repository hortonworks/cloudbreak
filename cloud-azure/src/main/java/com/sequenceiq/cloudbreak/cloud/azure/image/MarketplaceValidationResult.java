package com.sequenceiq.cloudbreak.cloud.azure.image;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

public class MarketplaceValidationResult {

    private final boolean fallbackRequired;

    private final boolean skipVhdCopy;

    private final ValidationResult validationResult;

    public MarketplaceValidationResult(boolean fallbackRequired, ValidationResult validationResult, boolean skipVhdCopy) {
        this.fallbackRequired = fallbackRequired;
        this.validationResult = validationResult;
        this.skipVhdCopy = skipVhdCopy;
    }

    public MarketplaceValidationResult(boolean fallbackRequired, boolean skipVhdCopy) {
        this.fallbackRequired = fallbackRequired;
        this.skipVhdCopy = skipVhdCopy;
        this.validationResult = null;
    }

    public boolean isFallbackRequired() {
        return fallbackRequired;
    }

    public boolean isSkipVhdCopy() {
        return skipVhdCopy;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public String toString() {
        return "MarketplaceValidationResult{" +
                "fallbackRequired=" + fallbackRequired +
                ", skipVhdCopy=" + skipVhdCopy +
                ", validationResult='" + validationResult + '\'' +
                '}';
    }
}