package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network;

import static java.util.Optional.ofNullable;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.network.InstanceGroupNetworkV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@Component
public class InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter
    extends AbstractConversionServiceAwareConverter<InstanceGroupNetwork, InstanceGroupNetworkV4Response> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroupNetworkV4Response convert(InstanceGroupNetwork source) {
        InstanceGroupNetworkV4Response response = new InstanceGroupNetworkV4Response();
        Json attributes = source.getAttributes();
        if (attributes != null) {
            Map<String, Object> parameters = attributes.getMap();
            ofNullable(source.getAttributes()).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
            providerParameterCalculator.parse(parameters, response);
        }
        return response;
    }

}
