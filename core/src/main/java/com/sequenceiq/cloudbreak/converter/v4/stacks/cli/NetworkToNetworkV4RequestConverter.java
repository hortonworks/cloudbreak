package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
@JsonInclude(Include.NON_NULL)
public class NetworkToNetworkV4RequestConverter extends AbstractConversionServiceAwareConverter<Network, NetworkV4Request> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public NetworkV4Request convert(Network source) {
        NetworkV4Request networkRequest = new NetworkV4Request();
        if (source.getAttributes() != null) {
            providerParameterCalculator.parse(cleanMap(source.getAttributes().getMap()), networkRequest);
        }
        networkRequest.setSubnetCIDR(source.getSubnetCIDR());
        return networkRequest;
    }
}
