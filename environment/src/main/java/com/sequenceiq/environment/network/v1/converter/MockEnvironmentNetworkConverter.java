package com.sequenceiq.environment.network.v1.converter;

import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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

    public MockEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter, EntitlementService entitlementService) {
        super(environmentViewConverter, entitlementService);
    }

    @Override
    public boolean isApplicableForDwx(CloudSubnet cloudSubnet) {
        return true;
    }

    @Override
    public boolean isApplicableForMlx(CloudSubnet cloudSubnet) {
        return true;
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
                        CreatedSubnet::getSubnetId, subnet ->
                                new CloudSubnet.Builder()
                                        .id(subnet.getSubnetId())
                                        .name(subnet.getSubnetId())
                                        .availabilityZone(subnet.getAvailabilityZone())
                                        .cidr(subnet.getCidr())
                                        .privateSubnet(!subnet.isPublicSubnet())
                                        .mapPublicIpOnLaunch(subnet.isMapPublicIpOnLaunch())
                                        .igwAvailable(subnet.isIgwAvailable())
                                        .type(subnet.getType())
                                        .deploymentRestrictions(subnet.isPublicSubnet()
                                                ? getDeploymentRestrictionWhenPublicSubnet(createdCloudNetwork)
                                                : getDeploymentRestrictionForPrivateSubnet(subnet.getType()))
                                        .build()
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
                MockParams.builder()
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
        return networkDto.getMock() != null && StringUtils.hasLength(networkDto.getMock().getVpcId());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.MOCK;
    }

}
