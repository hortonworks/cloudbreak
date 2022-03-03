package com.sequenceiq.environment.network.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureUtils.NETWORK_ID;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureUtils.PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureUtils.RG_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver;

    public AzureEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter,
        AzureRegistrationTypeResolver azureRegistrationTypeResolver) {
        super(environmentViewConverter);
        this.azureRegistrationTypeResolver = azureRegistrationTypeResolver;
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AzureNetwork azureNetwork = new AzureNetwork();
        AzureParams azureParams = network.getAzure();
        if (azureParams != null) {
            azureNetwork.setNetworkId(azureParams.getNetworkId());
            azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
            azureNetwork.setNoPublicIp(azureParams.isNoPublicIp());
            if (ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT.equals(network.getServiceEndpointCreation())) {
                azureNetwork.setPrivateDnsZoneId(azureParams.getPrivateDnsZoneId());
            }
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
                                subnet.isIgwAvailable(),
                                subnet.getType())
                        )
                )
        );
        return azureNetwork;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        return builder.withAzure(
                        AzureParams.builder()
                                .withNetworkId(azureNetwork.getNetworkId())
                                .withResourceGroupName(azureNetwork.getResourceGroupName())
                                .withNoPublicIp(azureNetwork.getNoPublicIp())
                                .withPrivateDnsZoneId(azureNetwork.getPrivateDnsZoneId())
                                .build())
                .build();
    }

    @Override
    void setRegistrationType(BaseNetwork result, NetworkDto networkDto) {
        result.setRegistrationType(azureRegistrationTypeResolver.getRegistrationType(networkDto));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public boolean isApplicableForDwx(CloudSubnet cloudSubnet) {
        return cloudSubnet.getType() == null || SubnetType.DWX.equals(cloudSubnet.getType());
    }

    @Override
    public boolean isApplicableForMlx(CloudSubnet cloudSubnet) {
        return cloudSubnet.getType() == null || SubnetType.MLX.equals(cloudSubnet.getType());
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        AzureNetwork azureNetwork = (AzureNetwork) baseNetwork;
        Map<String, Object> param = new HashMap<>();
        param.put(RG_NAME, azureNetwork.getResourceGroupName());
        param.put(NETWORK_ID, azureNetwork.getNetworkId());
        param.put(PRIVATE_DNS_ZONE_ID, azureNetwork.getPrivateDnsZoneId());
        return new Network(null, param);
    }
}
