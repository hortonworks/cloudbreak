package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.OutboundType;

@Service
public class AzureOutboundManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureOutboundManager.class);

    public CloudResourceStatus updateNetworkOutbound(CloudResource network, OutboundType outboundType) {
        NetworkAttributes networkAttributes = network.getTypedAttributes(NetworkAttributes.class, NetworkAttributes::new);
        if (networkAttributes == null) {
            LOGGER.error("Network attributes are not set for network: {}", network.getName());
            return new CloudResourceStatus(network, ResourceStatus.FAILED, "Network attributes are not set");
        } else {
            networkAttributes.setOutboundType(outboundType);
            network.setTypedAttributes(networkAttributes);
            LOGGER.info("Updated outbound type for network: {} to {}", network.getName(), outboundType);
            return new CloudResourceStatus(network, ResourceStatus.UPDATED);
        }
    }

    public boolean shouldSyncForOutbound(List<CloudResource> resources) {
        Optional<CloudResource> network = resources.stream()
                .filter(r -> r.getType() == AZURE_NETWORK)
                .findFirst();
        if (network.isEmpty()) {
            LOGGER.debug("No network resource found");
            return false;
        }

        NetworkAttributes networkAttributes = network.get().getTypedAttributes(NetworkAttributes.class, NetworkAttributes::new);
        boolean shouldSync = networkAttributes.getOutboundType().shouldSync();

        LOGGER.debug("Checking network for outbound type. Network available: true, outbound type: {}",
                networkAttributes.getOutboundType());
        return shouldSync;
    }
}
