package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FlexSubscriptionRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Component
public class FlexSubscriptionRequestToFlexSubscriptionConverter extends AbstractConversionServiceAwareConverter<FlexSubscriptionRequest, FlexSubscription> {

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Override
    public FlexSubscription convert(FlexSubscriptionRequest source) {
        FlexSubscription subscription = new FlexSubscription();
        subscription.setName(source.getName());
        subscription.setSubscriptionId(source.getSubscriptionId());
        subscription.setDefault(source.getUsedAsDefault());
        subscription.setUsedForController(source.isUsedForController());
        Long smartSenseSubscriptionId = source.getSmartSenseSubscriptionId();
        try {
            SmartSenseSubscription smartSenseSubscription = smartSenseSubscriptionService.findOneById(smartSenseSubscriptionId);
            subscription.setSmartSenseSubscription(smartSenseSubscription);
        } catch (NotFoundException ignored) {
            throw new BadRequestException("SmartSense subscription could not be found with id: " + smartSenseSubscriptionId);
        }
        return subscription;
    }
}
