package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAwsNetworkProvider implements FreeIpaNetworkProvider {

    @Override
    public NetworkRequest provider(EnvironmentDto environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        AwsParams awsParams = environment.getNetwork().getAws();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        awsNetworkParameters.setVpcId(awsParams.getVpcId());
        awsNetworkParameters.setSubnetId(environment.getNetwork().getSubnetMetas().keySet().iterator().next());
        networkRequest.setAws(awsNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        AwsNetworkParameters awsNetwork = networkRequest.getAws();
        return environment.getNetwork().getSubnetMetas().get(awsNetwork.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
