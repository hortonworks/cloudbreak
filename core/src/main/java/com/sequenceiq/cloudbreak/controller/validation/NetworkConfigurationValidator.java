package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Set;

import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Network;

@Component
public class NetworkConfigurationValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkConfigurationValidator.class);

    public boolean validateNetworkForStack(Network network, Set<InstanceGroup> instanceGroups) {
        SubnetUtils utils = new SubnetUtils(network.getSubnetCIDR());
        int addressCount = utils.getInfo().getAddressCount();
        int nodeCount = 0;
        for (InstanceGroup instanceGroup : instanceGroups) {
            nodeCount += instanceGroup.getNodeCount();
        }
        if (addressCount < nodeCount) {
            LOGGER.error("Cannot assign more than {} addresses in the selected subnet.", addressCount);
            throw new BadRequestException(
                    String.format("Cannot assign more than %s addresses in the selected subnet.", addressCount));
        }
        return true;
    }
}
