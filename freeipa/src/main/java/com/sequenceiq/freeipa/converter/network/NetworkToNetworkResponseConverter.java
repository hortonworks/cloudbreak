package com.sequenceiq.freeipa.converter.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkResponse;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class NetworkToNetworkResponseConverter implements Converter<Stack, NetworkResponse> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public NetworkResponse convert(Stack source) {
        NetworkResponse networkResp = null;
        Network network = source.getNetwork();
        if (network != null) {
            networkResp = new NetworkResponse();
            networkResp.setNetworkCidrs(network.getNetworkCidrs());
            networkResp.setOutboundInternetTraffic(network.getOutboundInternetTraffic());
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
