package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

public class SmartSenseSubscriptionView {

    private final String subscriptionId;

    public SmartSenseSubscriptionView(SmartSenseSubscription smartSenseSubscription) {
        this(smartSenseSubscription.getSubscriptionId());
    }

    public SmartSenseSubscriptionView(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

}
