package com.sequenceiq.environment.network.v1.converter;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AwsEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AwsNetwork awsNetwork = new AwsNetwork();
        if (network.getAws() != null) {
            awsNetwork.setVpcId(network.getAws().getVpcId());
        }
        return awsNetwork;
    }

    @Override
    public BaseNetwork setProviderSpecificNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        AwsNetwork awsNetwork = (AwsNetwork) baseNetwork;
        awsNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        awsNetwork.setVpcId(createdCloudNetwork.getNetworkId());
        awsNetwork.setSubnetIds(createdCloudNetwork.getSubnets().stream().map(CreatedSubnet::getSubnetId).collect(Collectors.toSet()));
        awsNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(
                        CreatedSubnet::getSubnetId,
                        subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone()))));
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
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
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

    @Override
    public boolean hasExistingNetwork(BaseNetwork baseNetwork) {
        return Optional.ofNullable((AwsNetwork) baseNetwork).map(AwsNetwork::getVpcId).isPresent();
    }
}
