package com.sequenceiq.environment.environment.converter.network;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAwsV1Params;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.environment.domain.network.AwsNetwork;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

@Component
public class AwsEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV1Request source) {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId(source.getAws().getVpcId());
        return awsNetwork;
    }

    @Override
    EnvironmentNetworkV1Response setProviderSpecificFields(EnvironmentNetworkV1Response result, BaseNetwork network) {
        AwsNetwork awsNetwork = (AwsNetwork) network;
        EnvironmentNetworkAwsV1Params awsV1Params = new EnvironmentNetworkAwsV1Params();
        awsV1Params.setVpcId(awsNetwork.getVpcId());
        result.setAws(awsV1Params);
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
