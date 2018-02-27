package com.sequenceiq.cloudbreak.blueprint;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@Component
public class SmartsenseConfigurationLocator {

    private static final String HST_SERVER_COMPONENT = "HST_SERVER";

    @Value("${cb.smartsense.getConfigurationEntries:false}")
    private boolean configureSmartSense;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    public boolean smartsenseConfigurableBySubscriptionId(String blueprintText, Optional<String> smartSenseSubscriptionId) {
        return configureSmartSense
                && blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprintText)
                && smartSenseSubscriptionId.isPresent();
    }

    public boolean smartsenseConfigurable(String blueprintText, Optional<SmartSenseSubscription> subscription) {
        return smartsenseConfigurableBySubscriptionId(
                blueprintText, subscription.isPresent() ? Optional.of(subscription.get().getSubscriptionId()) : Optional.empty());
    }
}
