package com.sequenceiq.environment.network.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.AwsNetwork;
import com.sequenceiq.environment.network.BaseNetwork;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId(network.getAws().getVpcId());
        return awsNetwork;
    }

    @Override
    EnvironmentNetworkResponse setProviderSpecificFields(EnvironmentNetworkResponse result, BaseNetwork network) {
        AwsNetwork awsNetwork = (AwsNetwork) network;
        EnvironmentNetworkAwsParams awsV1Params = new EnvironmentNetworkAwsParams();
        awsV1Params.setVpcId(awsNetwork.getVpcId());
        result.setAws(awsV1Params);
        return result;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.NetworkDtoBuilder builder, BaseNetwork network) {
        AwsNetwork awsNetwork = (AwsNetwork) network;
        return builder.withAws(
                AwsParams.AwsParamsBuilder.anAwsParams()
                        .withVpcId(awsNetwork.getVpcId())
                        .build())
                .build();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
