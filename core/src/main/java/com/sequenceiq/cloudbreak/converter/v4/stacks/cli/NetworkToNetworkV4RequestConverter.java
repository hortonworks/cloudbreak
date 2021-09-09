package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
@JsonInclude(Include.NON_NULL)
public class NetworkToNetworkV4RequestConverter {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    public NetworkV4Request convert(Network source) {
        NetworkV4Request networkRequest = new NetworkV4Request();
        if (source.getAttributes() != null) {
            providerParameterCalculator.parse(cleanMap(source.getAttributes().getMap()), networkRequest);
        }
        networkRequest.setSubnetCIDR(source.getSubnetCIDR());
        return networkRequest;
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
