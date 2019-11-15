package com.sequenceiq.environment.network.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.environment.CloudPlatform;
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
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        AwsNetwork awsNetwork = (AwsNetwork) baseNetwork;
        awsNetwork.setName(createdCloudNetwork.getStackName());
        awsNetwork.setVpcId(createdCloudNetwork.getNetworkId());
        awsNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(
                        CreatedSubnet::getSubnetId, subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone(),
                                subnet.getCidr(),
                                !subnet.isPublicSubnet(),
                                subnet.isMapPublicIpOnLaunch(),
                                subnet.isIgwAvailable())
                        )
                )
        );
        return awsNetwork;
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
    void setRegistrationType(BaseNetwork result, NetworkDto networkDto) {
        if (isExistingNetworkSpecified(networkDto)) {
            result.setRegistrationType(RegistrationType.EXISTING);
        } else {
            result.setRegistrationType(RegistrationType.CREATE_NEW);
        }
    }

    private boolean isExistingNetworkSpecified(NetworkDto networkDto) {
        return networkDto.getAws() != null && networkDto.getAws().getVpcId() != null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        AwsNetwork awsNetwork = (AwsNetwork) baseNetwork;
        Map<String, Object> param = new HashMap<>();
        param.put(AwsNetworkView.VPC_ID, awsNetwork.getVpcId());
        param.put(AwsNetworkView.REGION, awsNetwork.getEnvironments().stream().findFirst().get().getLocation());
        return new Network(null, param);
    }
}
