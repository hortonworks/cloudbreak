package com.sequenceiq.cloudbreak.converter.v4.felxsubscriptions;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscriptions.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;

@Component
public class FlexSubscriptionToFlexSubscriptionV4ResponseConverter extends AbstractConversionServiceAwareConverter<FlexSubscription, FlexSubscriptionV4Response> {

    @Inject
    private SmartSenseSubscriptionToSmartSenseSubscriptionJsonConverter smartSenseSubscriptionToSmartSenseSubscriptionJsonConverter;

    @Override
    public FlexSubscriptionV4Response convert(FlexSubscription source) {
        FlexSubscriptionV4Response json = new FlexSubscriptionV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setSubscriptionId(source.getSubscriptionId());
        json.setSmartSenseSubscriptionId(source.getSmartSenseSubscription().getId());
        SmartSenseSubscriptionJson smartSenseSubscriptionJson =
                smartSenseSubscriptionToSmartSenseSubscriptionJsonConverter.convert(source.getSmartSenseSubscription());
        json.setSmartSenseSubscription(smartSenseSubscriptionJson);
        json.setUsedAsDefault(source.isDefault());
        json.setUsedForController(source.isUsedForController());
        return json;
    }
}
