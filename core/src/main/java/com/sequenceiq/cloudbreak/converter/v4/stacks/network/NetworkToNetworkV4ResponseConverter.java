package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ParameterMapToClassConverterUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
@JsonInclude(Include.NON_NULL)
public class NetworkToNetworkV4ResponseConverter extends AbstractConversionServiceAwareConverter<Network, NetworkV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkToNetworkV4ResponseConverter.class);

    private ParameterMapToClassConverterUtil parameterMapToClassConverterUtil;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public NetworkV4Response convert(Network source) {
        NetworkV4Response networkRequest = new NetworkV4Response();
        if (source.getAttributes() != null) {
            providerParameterCalculator.to(cleanMap(source.getAttributes().getMap()), networkRequest);
        }
        networkRequest.setSubnetCIDR(source.getSubnetCIDR());
        return networkRequest;
    }
}
