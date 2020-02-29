package com.sequenceiq.environment.network.v1.converter;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.MockNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.MockParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class MockEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    public MockEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter,
            SubnetTypeConverter subnetTypeConverter) {
        super(environmentViewConverter, subnetTypeConverter);
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        MockNetwork mockNetwork = new MockNetwork();
        if (network.getMock() != null) {
            mockNetwork.setVpcId(network.getMock().getVpcId());
        }
        return mockNetwork;
    }

    @Override
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        MockNetwork mockNetwork = (MockNetwork) baseNetwork;
        mockNetwork.setName(createdCloudNetwork.getStackName());
        mockNetwork.setVpcId(createdCloudNetwork.getNetworkId());
        mockNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(
                        CreatedSubnet::getSubnetId, subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone(),
                                subnet.getCidr(),
                                !subnet.isPublicSubnet(),
                                subnet.isMapPublicIpOnLaunch(),
                                subnet.isIgwAvailable(),
                                subnet.getType())
                        )
                )
        );
        return mockNetwork;
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        Subnet subnet = new Subnet(baseNetwork.getNetworkCidr());
        return new Network(subnet, Collections.emptyMap());
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        MockNetwork mockNetwork = (MockNetwork) network;
        return builder.withMock(
                MockParams.MockParamsBuilder
                    .aMockParams()
                    .withVpcId(mockNetwork.getVpcId())
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
        return networkDto.getMock() != null && networkDto.getMock().getVpcId() != null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

}
