package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Component
public class JsonToFlexSubscriptionConverter extends AbstractConversionServiceAwareConverter<FlexSubscriptionRequest, FlexSubscription> {

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Override
    public FlexSubscription convert(FlexSubscriptionRequest source) {
        FlexSubscription subscription = new FlexSubscription();
        subscription.setName(source.getName());
        subscription.setSubscriptionId(source.getSubscriptionId());
        Long smartSenseSubscriptionId = source.getSmartSenseSubscriptionId();
        SmartSenseSubscription smartSenseSubscription = smartSenseSubscriptionService.findOneById(smartSenseSubscriptionId);
        subscription.setSmartSenseSubscription(smartSenseSubscription);
        return subscription;
    }
}
