package com.sequenceiq.environment.network.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.DWX;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.MLX;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PRIVATE;
import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class GcpEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    protected GcpEnvironmentNetworkConverter(EnvironmentViewConverter environmentViewConverter) {
        super(environmentViewConverter);
    }

    @Override
    public boolean isApplicableForDwx(CloudSubnet cloudSubnet) {
        return cloudSubnet.getType() == null
                || PUBLIC.equals(cloudSubnet.getType())
                || PRIVATE.equals(cloudSubnet.getType())
                || DWX.equals(cloudSubnet.getType());
    }

    @Override
    public boolean isApplicableForMlx(CloudSubnet cloudSubnet) {
        return cloudSubnet.getType() == null
                || PUBLIC.equals(cloudSubnet.getType())
                || PRIVATE.equals(cloudSubnet.getType())
                || MLX.equals(cloudSubnet.getType());
    }

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        GcpNetwork gcpNetwork = new GcpNetwork();
        GcpParams gcpParams = network.getGcp();
        if (gcpParams != null) {
            gcpNetwork.setNetworkId(gcpParams.getNetworkId());
            gcpNetwork.setNoFirewallRules(gcpParams.getNoFirewallRules());
            gcpNetwork.setNoPublicIp(gcpParams.getNoPublicIp());
            gcpNetwork.setSharedProjectId(gcpParams.getSharedProjectId());
        }
        return gcpNetwork;
    }

    @Override
    public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        GcpNetwork gcpNetwork = (GcpNetwork) baseNetwork;
        gcpNetwork.setNetworkId(createdCloudNetwork.getNetworkId());

        Map<String, Object> properties = createdCloudNetwork.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
        }

        Object sharedProjectId = properties.get("sharedProjectId");
        gcpNetwork.setSharedProjectId(sharedProjectId == null ? null : sharedProjectId.toString());

        Object noFirewallRules = properties.get("noFirewallRules");
        gcpNetwork.setNoFirewallRules(noFirewallRules != null && Boolean.parseBoolean(noFirewallRules.toString()));

        Object noPublicIp = properties.get("noPublicIp");
        gcpNetwork.setNoPublicIp(noPublicIp != null && Boolean.parseBoolean(noPublicIp.toString()));

        gcpNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(
                        CreatedSubnet::getSubnetId, subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone(),
                                subnet.getCidr(),
                                !subnet.isPublicSubnet(),
                                subnet.isMapPublicIpOnLaunch(),
                                subnet.isIgwAvailable(),
                                subnet.isIgwAvailable() ? PUBLIC : PRIVATE)
                        )
                )
        );
        return gcpNetwork;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        GcpNetwork gcpNetwork = (GcpNetwork) network;
        GcpParams.Builder gcpParamsBuilder = GcpParams.builder();
        Optional.ofNullable(gcpNetwork.getNetworkId()).ifPresent(gcpParamsBuilder::withNetworkId);
        Optional.ofNullable(gcpNetwork.getNoFirewallRules()).ifPresent(gcpParamsBuilder::withNoFirewallRules);
        Optional.ofNullable(gcpNetwork.getNoPublicIp()).ifPresent(gcpParamsBuilder::withNoPublicIp);
        Optional.ofNullable(gcpNetwork.getSharedProjectId()).ifPresent(gcpParamsBuilder::withSharedProjectId);
        return builder
                .withGcp(gcpParamsBuilder.build())
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
        return networkDto.getGcp() != null && networkDto.getGcp().getNetworkId() != null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public Network convertToNetwork(BaseNetwork baseNetwork) {
        GcpNetwork gcpNetwork = (GcpNetwork) baseNetwork;
        Map<String, Object> param = new HashMap<>();
        param.put(GcpStackUtil.NETWORK_ID, gcpNetwork.getNetworkId());
        param.put(GcpStackUtil.SHARED_PROJECT_ID, gcpNetwork.getSharedProjectId());
        param.put(GcpStackUtil.NO_PUBLIC_IP, gcpNetwork.getNoPublicIp());
        param.put(GcpStackUtil.NO_FIREWALL_RULES, gcpNetwork.getNoFirewallRules());
        param.put(GcpStackUtil.SUBNET_ID, baseNetwork.getSubnetMetas().entrySet().stream().findFirst().get().getKey());
        param.put(GcpStackUtil.REGION, baseNetwork.getEnvironments().stream().findFirst().get().getLocation());
        return new Network(null, param);
    }
}
