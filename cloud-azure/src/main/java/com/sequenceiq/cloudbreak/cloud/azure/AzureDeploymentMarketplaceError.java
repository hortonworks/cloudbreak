package com.sequenceiq.cloudbreak.cloud.azure;

public enum AzureDeploymentMarketplaceError {

    AUTHORIZATION_FAILED("AuthorizationFailed", "Microsoft.Marketplace"),

    MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED("MarketplacePurchaseEligibilityFailed", "");

    private final String code;

    private final String messageFragment;

    AzureDeploymentMarketplaceError(String code, String messageFragment) {
        this.code = code;
        this.messageFragment = messageFragment;
    }

    public String value() {
        return code;
    }

    public String getMessageFragment() {
        return messageFragment;
    }

    public String getCode() {
        return code;
    }
}
