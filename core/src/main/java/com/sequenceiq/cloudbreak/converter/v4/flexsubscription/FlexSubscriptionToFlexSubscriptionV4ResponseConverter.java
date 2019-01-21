package com.sequenceiq.cloudbreak.converter.v4.flexsubscription;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;

@Component
public class FlexSubscriptionToFlexSubscriptionV4ResponseConverter extends
        AbstractConversionServiceAwareConverter<FlexSubscription, FlexSubscriptionV4Response> {

    @Override
    public FlexSubscriptionV4Response convert(FlexSubscription source) {
        FlexSubscriptionV4Response json = new FlexSubscriptionV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setSubscriptionId(source.getSubscriptionId());
        json.setSmartSenseSubscriptionId(source.getSmartSenseSubscription().getId());
        SmartSenseSubscriptionV4Response smartSenseSubscriptionV4Response =
                getConversionService().convert(source.getSmartSenseSubscription(), SmartSenseSubscriptionV4Response.class);
        json.setSmartSenseSubscription(smartSenseSubscriptionV4Response);
        json.setUsedAsDefault(source.isDefault());
        json.setUsedForController(source.isUsedForController());
        return json;
    }
}
