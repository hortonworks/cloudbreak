package com.sequenceiq.cloudbreak.converter.v4.flexsubscription;


import javax.inject.Inject;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.requests.FlexSubscriptionV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Component
public class FlexSubscriptionV4RequestToFlexSubscriptionConverter extends AbstractConversionServiceAwareConverter<FlexSubscriptionV4Request, FlexSubscription> {

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Override
    public FlexSubscription convert(FlexSubscriptionV4Request source) {
        FlexSubscription subscription = new FlexSubscription();
        subscription.setName(source.getName());
        subscription.setSubscriptionId(source.getSubscriptionId());
        subscription.setDefault(source.getUsedAsDefault());
        subscription.setUsedForController(source.isUsedForController());
        Long smartSenseSubscriptionId = source.getSmartSenseSubscriptionId();
        try {
            SmartSenseSubscription smartSenseSubscription = smartSenseSubscriptionService.findById(smartSenseSubscriptionId);
            subscription.setSmartSenseSubscription(smartSenseSubscription);
        } catch (AccessDeniedException | NotFoundException ignored) {
            throw new BadRequestException("SmartSense subscription could not be found with id or access is denied: " + smartSenseSubscriptionId);
        }
        return subscription;
    }
}
