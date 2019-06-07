package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.domain.AwsNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAwsNetworkProvider implements FreeIpaNetworkProvider {

    @Override
    public NetworkRequest provider(Environment environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        AwsNetwork network = (AwsNetwork) environment.getNetwork();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        awsNetworkParameters.setVpcId(network.getVpcId());
        awsNetworkParameters.setSubnetId(network.getSubnetIdsSet().iterator().next());
        networkRequest.setAws(awsNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, Environment environment) {
        AwsNetworkParameters awsNetwork = networkRequest.getAws();
        return environment.getNetwork().getSubnetMetasMap().get(awsNetwork.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
