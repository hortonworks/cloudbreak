package com.sequenceiq.environment.network.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AzureNetwork azureNetwork = new AzureNetwork();
        AzureParams azureParams = network.getAzure();
        if (azureParams != null) {
            azureNetwork.setNetworkId(azureParams.getNetworkId());
            azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
            azureNetwork.setNoPublicIp(azureParams.isNoPublicIp());
            azureNetwork.setNoFirewallRules(azureParams.isNoFirewallRules());
        }
        return azureNetwork;
    }

    @Override
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        AzureNetwork azureNetwork = (AzureNetwork) baseNetwork;
        azureNetwork.setName(createdCloudNetwork.getStackName());
        azureNetwork.setNetworkId(createdCloudNetwork.getNetworkId());
        azureNetwork.setResourceGroupName(String.valueOf(createdCloudNetwork.getProperties().get("resourceGroupName")));
        azureNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(CreatedSubnet::getSubnetId,
                        subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone(),
                                subnet.getCidr(),
                                subnet.isPublicSubnet(),
                                subnet.isMapPublicIpOnLaunch(),
                                subnet.isIgwAvailable())
                        )
                )
        );
        return azureNetwork;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        return builder.withAzure(
                AzureParams.AzureParamsBuilder
                    .anAzureParams()
                    .withNetworkId(azureNetwork.getNetworkId())
                    .withResourceGroupName(azureNetwork.getResourceGroupName())
                    .withNoFirewallRules(azureNetwork.getNoFirewallRules())
                    .withNoPublicIp(azureNetwork.getNoPublicIp())
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
        return networkDto.getAzure() != null && networkDto.getAzure().getNetworkId() != null && networkDto.getAzure().getResourceGroupName() != null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        AzureNetwork azureNetwork = (AzureNetwork) baseNetwork;
        Map<String, Object> param = new HashMap<>();
        param.put(AzureUtils.RG_NAME, azureNetwork.getResourceGroupName());
        param.put(AzureUtils.NETWORK_ID, azureNetwork.getNetworkId());
        return new Network(null, param);
    }
}
