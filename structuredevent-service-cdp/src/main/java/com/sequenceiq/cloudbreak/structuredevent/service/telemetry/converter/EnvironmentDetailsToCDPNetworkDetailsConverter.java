package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class EnvironmentDetailsToCDPNetworkDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPNetworkDetailsConverter.class);

    public UsageProto.CDPNetworkDetails convert(EnvironmentDetails environmentDetails) {
        UsageProto.CDPNetworkDetails.Builder cdpNetworkDetails = UsageProto.CDPNetworkDetails.newBuilder();

        Tunnel tunnel = environmentDetails.getTunnel();
        if (tunnel != null) {
            cdpNetworkDetails.setConnectivity(environmentDetails.getTunnel().name());
        }

        NetworkDto network = environmentDetails.getNetwork();
        if (network != null) {
            cdpNetworkDetails.setNetworkType(network.getRegistrationType().name());
            cdpNetworkDetails.setServiceEndpointCreation(network.getServiceEndpointCreation().name());
            if (network.getSubnetMetas() != null) {
                List<SubnetType> types = network.getSubnetMetas().values().stream().map(CloudSubnet::getType)
                        .filter(Objects::nonNull).sorted().collect(Collectors.toUnmodifiableList());
                cdpNetworkDetails.setNumberPrivateSubnets(
                        types.stream()
                                .filter(e -> e.equals(SubnetType.PRIVATE) || e.equals(SubnetType.MLX) || e.equals(SubnetType.DWX))
                                .collect(Collectors.toList())
                                .size());
                cdpNetworkDetails.setNumberPublicSubnets(
                        types.stream()
                                .filter(e -> e.equals(SubnetType.PUBLIC))
                                .collect(Collectors.toList())
                                .size());
            }

            cdpNetworkDetails.setPublicEndpointAccessGateway(network.getPublicEndpointAccessGateway() != null ?
                    network.getPublicEndpointAccessGateway().name() : PublicEndpointAccessGateway.DISABLED.name());
        }

        UsageProto.CDPProxyDetails.Builder cdpProxyDetails = UsageProto.CDPProxyDetails.newBuilder();
        cdpProxyDetails.setProxy(environmentDetails.getProxyConfigConfigured());
        cdpNetworkDetails.setProxyDetails(cdpProxyDetails.build());

        UsageProto.CDPNetworkDetails ret = cdpNetworkDetails.build();
        LOGGER.debug("Converted CDPNetworkDetails: {}", ret);
        return ret;
    }
}
