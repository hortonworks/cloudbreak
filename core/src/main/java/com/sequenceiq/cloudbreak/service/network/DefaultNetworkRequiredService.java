package com.sequenceiq.cloudbreak.service.network;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Service
public class DefaultNetworkRequiredService {

    public boolean shouldAddNetwork(String cloudPlatform, NetworkV4Request network) {
        if (network == null) {
            return true;
        } else if (isAwsNetworkNullAndProviderIsAws(cloudPlatform, network)) {
            return true;
        } else if (isAzureNetworkNullAndProviderIsAzure(cloudPlatform, network)) {
            return true;
        }
        return false;
    }

    private boolean isAwsNetworkNullAndProviderIsAws(String cloudPlatform, NetworkV4Request network) {
        return CloudPlatform.AWS.name().equals(cloudPlatform)
                && (network.getAws() == null || allTheAwsValueNull(network));
    }

    private boolean allTheAwsValueNull(NetworkV4Request network) {
        return Strings.isNullOrEmpty(network.getAws().getInternetGatewayId())
                && Strings.isNullOrEmpty(network.getAws().getVpcId());
    }

    private boolean isAzureNetworkNullAndProviderIsAzure(String cloudPlatform, NetworkV4Request network) {
        return CloudPlatform.AZURE.name().equals(cloudPlatform)
                && (network.getAzure() == null || allTheAzureValueNull(network));
    }

    private boolean allTheAzureValueNull(NetworkV4Request network) {
        return Strings.isNullOrEmpty(network.getAzure().getNetworkId())
                && Strings.isNullOrEmpty(network.getAzure().getResourceGroupName());
    }

}
