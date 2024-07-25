package com.sequenceiq.environment.network.v1.converter;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.AKS_PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.DATABASE_PRIVATE_DNS_ZONE_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NO_OUTBOUND_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.USE_PUBLIC_DNS_FOR_PRIVATE_AKS;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.AzureRegistrationTypeResolver;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    private final AzureRegistrationTypeResolver azureRegistrationTypeResolver;

    private final AvailabilityZoneConverter availabilityZoneConverter;

    public AzureEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter, EntitlementService entitlementService,
            AzureRegistrationTypeResolver azureRegistrationTypeResolver, AvailabilityZoneConverter availabilityZoneConverter) {
        super(environmentViewConverter, entitlementService);
        this.azureRegistrationTypeResolver = azureRegistrationTypeResolver;
        this.availabilityZoneConverter = availabilityZoneConverter;
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AzureNetwork azureNetwork = new AzureNetwork();
        AzureParams azureParams = network.getAzure();
        if (azureParams != null) {
            azureNetwork.setNetworkId(azureParams.getNetworkId());
            azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
            azureNetwork.setNoPublicIp(azureParams.isNoPublicIp());
            azureNetwork.setAksPrivateDnsZoneId(azureParams.getAksPrivateDnsZoneId());
            azureNetwork.setNoOutboundLoadBalancer(azureParams.isNoOutboundLoadBalancer());
            azureNetwork.setDatabasePrivateDnsZoneId(azureParams.getDatabasePrivateDnsZoneId());
            azureNetwork.setZoneMetas(availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(azureParams.getAvailabilityZones(),
                    azureNetwork.getZoneMetas()));
            azureNetwork.setFlexibleServerSubnetIds(azureParams.getFlexibleServerSubnetIds());
            azureNetwork.setUsePublicDnsForPrivateAks(azureParams.isUsePublicDnsForPrivateAks());
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
                                        subnet.getType(),
                                        subnet.isPublicSubnet()
                                                ? getDeploymentRestrictionWhenPublicSubnet(createdCloudNetwork)
                                                : getDeploymentRestrictionForPrivateSubnet(subnet.getType()))
                        )
                )
        );
        return azureNetwork;
    }

    @Override
    protected Set<DeploymentRestriction> getDeploymentRestrictionForPrivateSubnet(SubnetType type) {
        if (type == SubnetType.DWX) {
            return Set.of(DeploymentRestriction.DWX);
        } else if (type == SubnetType.MLX) {
            return Set.of(DeploymentRestriction.MLX);
        } else {
            return super.getDeploymentRestrictionForPrivateSubnet(type);
        }
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        return builder.withAzure(
                        AzureParams.builder()
                                .withNetworkId(azureNetwork.getNetworkId())
                                .withResourceGroupName(azureNetwork.getResourceGroupName())
                                .withNoPublicIp(azureNetwork.getNoPublicIp())
                                .withDatabasePrivateDnsZoneId(azureNetwork.getDatabasePrivateDnsZoneId())
                                .withAksPrivateDnsZoneId(azureNetwork.getAksPrivateDnsZoneId())
                                .withNoOutboundLoadBalancer(azureNetwork.isNoOutboundLoadBalancer())
                                .withAvailabilityZones(availabilityZoneConverter.getAvailabilityZonesFromJsonAttributes(azureNetwork.getZoneMetas()))
                                .withFlexibleServerSubnetIds(azureNetwork.getFlexibleServerSubnetIds())
                                .withUsePublicDnsForPrivateAks(azureNetwork.isUsePublicDnsForPrivateAks())
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
        param.put(RESOURCE_GROUP_NAME, azureNetwork.getResourceGroupName());
        param.put(NETWORK_ID, azureNetwork.getNetworkId());
        param.put(DATABASE_PRIVATE_DNS_ZONE_ID, azureNetwork.getDatabasePrivateDnsZoneId());
        param.put(AKS_PRIVATE_DNS_ZONE_ID, azureNetwork.getAksPrivateDnsZoneId());
        param.put(NO_OUTBOUND_LOAD_BALANCER, azureNetwork.isNoOutboundLoadBalancer());
        param.put(USE_PUBLIC_DNS_FOR_PRIVATE_AKS, azureNetwork.isUsePublicDnsForPrivateAks());
        return new Network(null, param);
    }

    @Override
    public NetworkDto.Builder extendBuilderWithProviderSpecificParameters(NetworkDto.Builder networkDtoBuilder, NetworkDto originalNetworkDto,
            NetworkDto newNetworkDto) {
        AzureParams newAzureParams = newNetworkDto.getAzure();
        AzureParams.Builder azureParamsBuilder = AzureParams.builder(originalNetworkDto.getAzure());
        if (Objects.nonNull(newAzureParams)) {
            if (StringUtils.isNotEmpty(newAzureParams.getDatabasePrivateDnsZoneId())) {
                azureParamsBuilder.withDatabasePrivateDnsZoneId(newAzureParams.getDatabasePrivateDnsZoneId());
            }
            if (CollectionUtils.isNotEmpty(newAzureParams.getFlexibleServerSubnetIds())) {
                azureParamsBuilder.withFlexibleServerSubnetIds(newAzureParams.getFlexibleServerSubnetIds());
            }
        }
        return networkDtoBuilder.withAzure(azureParamsBuilder.build());
    }

    @Override
    public void updateAvailabilityZones(BaseNetwork baseNetwork, Set<String> availabilityZones) {
        if (CollectionUtils.isNotEmpty(availabilityZones)) {
            AzureNetwork azureNetwork = (AzureNetwork) baseNetwork;
            azureNetwork.setZoneMetas(availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(availabilityZones, azureNetwork.getZoneMetas()));
        }
    }

    @Override
    public void updateProviderSpecificParameters(BaseNetwork baseNetwork, NetworkDto networkDto) {
        AzureNetwork azureNetwork = (AzureNetwork) baseNetwork;
        if (Objects.nonNull(networkDto.getAzure())) {
            azureNetwork.setFlexibleServerSubnetIds(networkDto.getAzure().getFlexibleServerSubnetIds());
            azureNetwork.setDatabasePrivateDnsZoneId(networkDto.getAzure().getDatabasePrivateDnsZoneId());
        }
        azureNetwork.setServiceEndpointCreation(networkDto.getServiceEndpointCreation());
    }
}