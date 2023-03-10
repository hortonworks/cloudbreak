package com.sequenceiq.environment.api.doc.marketplace;

public class MarketplaceDescription {

    public static final String GET = "get automatic marketplace image terms acceptance setting";

    public static final String GET_NOTES = "CDP is capable to automatically accept Azure Marketplace image terms " +
            "during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.";

    public static final String PUT = "update automatic marketplace image terms acceptance setting";

    public static final String PUT_NOTES = "CDP is capable to automatically accept Azure Marketplace image terms " +
            "during cluster deployment. You can use this setting in your account to opt in or opt out this behaviour.";

    private MarketplaceDescription() {
    }
}
