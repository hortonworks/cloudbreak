package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import static com.sequenceiq.cloudbreak.util.MapUtil.cleanMap;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
@JsonInclude(Include.NON_NULL)
public class NetworkToNetworkV4RequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkToNetworkV4RequestConverter.class);

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    public NetworkV4Request convert(Network source) {
        throwIfNull(source, () -> new IllegalStateException(Network.class.getSimpleName() + " should not be null!"));
        NetworkV4Request networkRequest = new NetworkV4Request();
        if (source.getAttributes() != null) {
            providerParameterCalculator.parse(cleanMap(source.getAttributes().getMap()), networkRequest);
        }
        networkRequest.setSubnetCIDR(source.getSubnetCIDR());
        if (networkRequest.isEmpty()) {
            LOGGER.debug("The created {} is empty, therefore null shall return.", NetworkV4Request.class.getSimpleName());
            return null;
        }
        LOGGER.debug("The following {} has created: {}", NetworkV4Request.class.getSimpleName(), networkRequest);
        return networkRequest;
    }

}
