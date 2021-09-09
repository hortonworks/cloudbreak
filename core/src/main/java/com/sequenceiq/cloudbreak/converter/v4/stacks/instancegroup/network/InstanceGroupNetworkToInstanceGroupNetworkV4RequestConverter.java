package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network;

import static java.util.Optional.ofNullable;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@Component
public class InstanceGroupNetworkToInstanceGroupNetworkV4RequestConverter {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    public InstanceGroupNetworkV4Request convert(InstanceGroupNetwork source) {
        InstanceGroupNetworkV4Request response = new InstanceGroupNetworkV4Request();
        Json attributes = source.getAttributes();
        if (attributes != null) {
            Map<String, Object> parameters = attributes.getMap();
            ofNullable(attributes).ifPresent(attr -> parameters.putAll(new Json(attr).getMap()));
            providerParameterCalculator.parse(parameters, response);
        }
        return response;
    }

}
