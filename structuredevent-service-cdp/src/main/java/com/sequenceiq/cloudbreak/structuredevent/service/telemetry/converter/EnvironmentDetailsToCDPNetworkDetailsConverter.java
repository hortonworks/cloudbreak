package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPOwnDnsZones;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPProxyDetails;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy.ProxyDetails;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class EnvironmentDetailsToCDPNetworkDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPNetworkDetailsConverter.class);

    private static final int DEFAULT_INTEGER_VALUE = -1;

    private static final int ZERO_INTEGER_VALUE = 0;

    public CDPNetworkDetails convert(EnvironmentDetails environmentDetails) {
        CDPNetworkDetails.Builder cdpNetworkDetails = CDPNetworkDetails.newBuilder();
        cdpNetworkDetails.setNumberPublicSubnets(DEFAULT_INTEGER_VALUE);
        cdpNetworkDetails.setNumberPrivateSubnets(DEFAULT_INTEGER_VALUE);

        Tunnel tunnel = environmentDetails.getTunnel();
        if (tunnel != null) {
            cdpNetworkDetails.setConnectivity(tunnel.name());
        }

        CcmV2TlsType tlsType = environmentDetails.getTlsType();
        if (tlsType != null) {
            cdpNetworkDetails.setControlPlaneAndCCMAgentConnectionSecurity(tlsType.name());
        }

        NetworkDto network = environmentDetails.getNetwork();
        if (network != null) {
            cdpNetworkDetails.setNetworkType(network.getRegistrationType().name());
            cdpNetworkDetails.setServiceEndpointCreation(network.getServiceEndpointCreation().name());
            setupNetworks(environmentDetails.getCloudPlatform(), cdpNetworkDetails, network);
            setupLoadBalancer(cdpNetworkDetails, network);
            cdpNetworkDetails.setPublicEndpointAccessGateway(network.getPublicEndpointAccessGateway() != null ?
                    network.getPublicEndpointAccessGateway().name() : PublicEndpointAccessGateway.DISABLED.name());
            cdpNetworkDetails.setNumberFlexibleServerSubnetIds(Optional.ofNullable(network.getAzure())
                    .map(n -> Optional.ofNullable(n.getFlexibleServerSubnetIds())
                            .map(Set::size)
                            .orElse(0))
                    .orElse(0));
        }
        cdpNetworkDetails.setSecurityAccessType(defaultIfEmpty(environmentDetails.getSecurityAccessType(), ""));
        cdpNetworkDetails.setProxyDetails(convertProxy(environmentDetails.getProxyDetails()));
        cdpNetworkDetails.setDomain(defaultIfEmpty(environmentDetails.getDomain(), ""));
        cdpNetworkDetails.setOwnDnsZones(convertOwnDnsZones(network, environmentDetails.getCloudPlatform()));

        CDPNetworkDetails ret = cdpNetworkDetails.build();
        LOGGER.debug("Converted CDPNetworkDetails: {}", ret);
        return ret;
    }

    private void setupNetworks(String cloudPlatform, CDPNetworkDetails.Builder cdpNetworkDetails, NetworkDto network) {
        if (network.getSubnetMetas() != null) {
            if (network.getSubnetMetas().isEmpty()) {
                cdpNetworkDetails.setNumberPrivateSubnets(0);
                cdpNetworkDetails.setNumberPublicSubnets(0);
            } else {
                List<SubnetType> types = network.getSubnetMetas().values().stream().map(CloudSubnet::getType)
                        .filter(Objects::nonNull).sorted().toList();
                if (!types.isEmpty()) {
                    cdpNetworkDetails.setNumberPrivateSubnets(
                            types.stream()
                                    .filter(e -> e.equals(SubnetType.PRIVATE) || e.equals(SubnetType.MLX) || e.equals(SubnetType.DWX))
                                    .toList()
                                    .size());
                    cdpNetworkDetails.setNumberPublicSubnets(
                            types.stream()
                                    .filter(e -> e.equals(SubnetType.PUBLIC))
                                    .toList()
                                    .size());
                } else {
                    //In case type is not defined (Azure & Mock by design), # of private Subnets can be updated, public subnets are zero
                    if (AZURE.equalsIgnoreCase(cloudPlatform)) {
                        int numberPrivateSubnets = network.getSubnetMetas().size();
                        cdpNetworkDetails.setNumberPrivateSubnets(numberPrivateSubnets);
                        cdpNetworkDetails.setNumberPublicSubnets(ZERO_INTEGER_VALUE);
                    }
                }
            }
        }
    }

    private void setupLoadBalancer(CDPNetworkDetails.Builder cdpNetworkDetails, NetworkDto network) {
        if (network.getEndpointGatewaySubnetIds() != null) {
            if (network.getEndpointGatewaySubnetIds().isEmpty()) {
                cdpNetworkDetails.setNumberPrivateLoadBalancerSubnets(0);
                cdpNetworkDetails.setNumberPublicLoadBalancerSubnets(0);
            } else {
                List<SubnetType> types = network.getEndpointGatewaySubnetMetas().values().stream().map(CloudSubnet::getType)
                        .filter(Objects::nonNull).sorted().toList();
                cdpNetworkDetails.setNumberPrivateLoadBalancerSubnets(
                        types.stream()
                                .filter(e -> e.equals(SubnetType.PRIVATE))
                                .toList()
                                .size()
                );
                cdpNetworkDetails.setNumberPublicLoadBalancerSubnets(
                        types.stream()
                                .filter(e -> e.equals(SubnetType.PUBLIC))
                                .toList()
                                .size()
                );
            }
        }
    }

    private CDPProxyDetails convertProxy(ProxyDetails proxyDetails) {
        CDPProxyDetails.Builder cdpProxyDetailsBuilder = CDPProxyDetails.newBuilder();
        if (proxyDetails != null) {
            cdpProxyDetailsBuilder.setProxy(proxyDetails.isEnabled());
            cdpProxyDetailsBuilder.setProtocol(proxyDetails.getProtocol());
            cdpProxyDetailsBuilder.setAuthentication(proxyDetails.getAuthentication());
        }
        return cdpProxyDetailsBuilder.build();
    }

    private CDPOwnDnsZones convertOwnDnsZones(NetworkDto networkDto, String cloudPlatform) {
        CDPOwnDnsZones.Builder builder = CDPOwnDnsZones.newBuilder();
        if (networkDto == null || cloudPlatform == null) {
            return builder.build();
        }

        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            boolean postgresPrivateDnsZonePresent = Optional.ofNullable(networkDto.getAzure())
                    .map(AzureParams::getDatabasePrivateDnsZoneId)
                    .map(StringUtils::isNotEmpty)
                    .orElse(false);
            builder.setPostgres(postgresPrivateDnsZonePresent);
        } else {
            LOGGER.debug("CloudPlatform is not azure thus not reading info on DNS zones");
        }
        return builder.build();
    }
}
