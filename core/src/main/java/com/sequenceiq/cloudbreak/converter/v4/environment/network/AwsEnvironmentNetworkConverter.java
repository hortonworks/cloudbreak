package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAwsV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.domain.environment.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;

@Component
public class AwsEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source) {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId(source.getAws().getVpcId());
        return awsNetwork;
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source, CreatedCloudNetwork createdCloudNetwork) {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId(createdCloudNetwork.getNetworkId());
        return awsNetwork;
    }

    @Override
    EnvironmentNetworkV4Response setProviderSpecificFields(EnvironmentNetworkV4Response result, BaseNetwork network) {
        AwsNetwork awsNetwork = (AwsNetwork) network;
        EnvironmentNetworkAwsV4Params awsV4Params = new EnvironmentNetworkAwsV4Params();
        awsV4Params.setVpcId(awsNetwork.getVpcId());
        result.setAws(awsV4Params);
        return result;
    }

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(BaseNetwork source) {
        AwsNetwork awsNetwork = (AwsNetwork) source;
        return Map.of("vpcId", awsNetwork.getVpcId());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public boolean hasExistingNetwork(EnvironmentNetworkV4Request source) {
        return Optional.of(source).map(EnvironmentNetworkV4Request::getAws).map(EnvironmentNetworkAwsV4Params::getVpcId).isPresent();
    }
}
