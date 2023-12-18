package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.apache.commons.lang3.StringUtils;

public class MarketplaceValidationResult {

    private final boolean fallbackRequired;

    private final boolean skipVhdCopy;

    private final String validationErrorMessage;

    public MarketplaceValidationResult(boolean fallbackRequired, String validationErrorMessage) {
        this.fallbackRequired = fallbackRequired;
        this.validationErrorMessage = validationErrorMessage;
        this.skipVhdCopy = StringUtils.isEmpty(validationErrorMessage);
    }

    public MarketplaceValidationResult(boolean fallbackRequired, boolean skipVhdCopy) {
        this.fallbackRequired = fallbackRequired;
        this.skipVhdCopy = skipVhdCopy;
        this.validationErrorMessage = null;
    }

    public boolean isFallbackRequired() {
        return fallbackRequired;
    }

    public boolean isSkipVhdCopy() {
        return skipVhdCopy;
    }

    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    @Override
    public String toString() {
        return "MarketplaceValidationResult{" +
                "fallbackRequired=" + fallbackRequired +
                ", skipVhdCopy=" + skipVhdCopy +
                ", validationMessage='" + validationErrorMessage + '\'' +
                '}';
    }
}
