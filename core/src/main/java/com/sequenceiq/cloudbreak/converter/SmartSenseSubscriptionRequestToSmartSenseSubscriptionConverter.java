package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@Component
public class SmartSenseSubscriptionRequestToSmartSenseSubscriptionConverter
        extends AbstractConversionServiceAwareConverter<SmartSenseSubscriptionJson, SmartSenseSubscription> {

    @Override
    public SmartSenseSubscription convert(SmartSenseSubscriptionJson source) {
        SmartSenseSubscription obj = new SmartSenseSubscription();
        obj.setSubscriptionId(source.getSubscriptionId());
        return obj;
    }
}
