package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.NetworkRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class NetworkV2RequestToNetworkRequestConverter extends AbstractConversionServiceAwareConverter<NetworkV2Request, NetworkRequest> {
    @Inject
    private TopologyService topologyService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public NetworkRequest convert(NetworkV2Request source) {
        NetworkRequest network = new NetworkRequest();
        network.setName(missingResourceNameGenerator.generateName(APIResourceType.NETWORK));
        network.setSubnetCIDR(source.getSubnetCIDR());
        network.setParameters(source.getParameters());
        return network;
    }
}
