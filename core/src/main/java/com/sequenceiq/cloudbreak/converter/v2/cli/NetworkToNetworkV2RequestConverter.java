package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
@JsonInclude(Include.NON_NULL)
public class NetworkToNetworkV2RequestConverter extends AbstractConversionServiceAwareConverter<Network, NetworkV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkToNetworkV2RequestConverter.class);

    @Override
    public NetworkV2Request convert(Network source) {
        NetworkV2Request networkV2Request = new NetworkV2Request();
        if (source.getAttributes() != null) {
            networkV2Request.setParameters(cleanMap(source.getAttributes().getMap()));
        }
        networkV2Request.setSubnetCIDR(source.getSubnetCIDR());
        return networkV2Request;
    }
}
