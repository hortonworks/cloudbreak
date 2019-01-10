package com.sequenceiq.cloudbreak.converter.v4.smartsense;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;

@Component
public class SmartSenseSubscriptionV4RequestToSmartSenseSubscriptionConverter
        extends AbstractConversionServiceAwareConverter<SmartSenseSubscriptionV4Response, SmartSenseSubscription> {

    @Override
    public SmartSenseSubscription convert(SmartSenseSubscriptionV4Response source) {
        SmartSenseSubscription obj = new SmartSenseSubscription();
        obj.setSubscriptionId(source.getSubscriptionId());
        return obj;
    }
}
