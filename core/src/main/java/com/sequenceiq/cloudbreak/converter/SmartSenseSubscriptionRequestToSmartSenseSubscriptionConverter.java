package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import org.springframework.stereotype.Component;

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
