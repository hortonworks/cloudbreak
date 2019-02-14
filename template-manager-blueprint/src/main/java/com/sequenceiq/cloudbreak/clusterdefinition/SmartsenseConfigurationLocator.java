package com.sequenceiq.cloudbreak.clusterdefinition;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@Component
public class SmartsenseConfigurationLocator {

    private static final String HST_SERVER_COMPONENT = "HST_SERVER";

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    public boolean smartsenseConfigurableBySubscriptionId(Optional<String> smartSenseSubscriptionId) {
        return configureSmartSense && smartSenseSubscriptionId.isPresent();
    }

    public boolean smartsenseConfigurable(Optional<SmartSenseSubscription> subscription) {
        return smartsenseConfigurableBySubscriptionId(subscription.isPresent() ? Optional.of(subscription.get().getSubscriptionId()) : Optional.empty());
    }
}
