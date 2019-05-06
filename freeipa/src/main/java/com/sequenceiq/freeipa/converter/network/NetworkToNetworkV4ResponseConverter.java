package com.sequenceiq.freeipa.converter.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class NetworkToNetworkV4ResponseConverter implements Converter<Stack, NetworkV4Response> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public NetworkV4Response convert(Stack source) {
        NetworkV4Response networkResp = null;
        Network network = source.getNetwork();
        if (network != null) {
            networkResp = new NetworkV4Response();
            if (network.getAttributes() != null) {
                Map<String, Object> parameters = cleanMap(network.getAttributes().getMap());
                providerParameterCalculator.parse(parameters, networkResp);
            }
        }
        return networkResp;
    }

    private Map<String, Object> cleanMap(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (!Objects.isNull(input.get(entry.getKey()))
                    && !"null".equals(input.get(entry.getKey()))
                    && !Strings.isNullOrEmpty(input.get(entry.getKey()).toString())) {
                result.put(entry.getKey(), input.get(entry.getKey()));
            }
        }
        return result;
    }
}
