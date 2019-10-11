package com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

class FillSubnetStrategy extends AzureSubnetStrategy {

    FillSubnetStrategy(List<String> subnets, Map<String, Long> availableIPs) {
        super(subnets, availableIPs);
    }

    @Override
    public String getNextSubnetId() {
        if (!getSubnets().isEmpty()) {
            for (String subnet : getSubnets()) {
                if (getAvailableIPs().get(subnet) > 0) {
                    getAvailableIPs().put(subnet, getAvailableIPs().get(subnet) - 1);
                    return subnet;
                }
            }
            throw new CloudConnectorException("Not enough IP address in the subnet(s)");
        }
        return null;
    }
}
