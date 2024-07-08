package com.sequenceiq.redbeams.service.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.ServiceEndpointCreationToEndpointTypeConverter;
import com.sequenceiq.common.model.PrivateEndpointType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class NetworkParameterAdder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkParameterAdder.class);

    // These constants must match those in AwsNetworkView, AzureNetworkView and/or GcpDatabaseNetworkView
    private static final String VPC_ID = "vpcId";

    private static final String VPC_CIDR = "vpcCidr";

    private static final String SHARED_PROJECT_ID = "sharedProjectId";

    private static final String VPC_CIDRS = "vpcCidrs";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String ENDPOINT_TYPE = "endpointType";

    private static final String SUBNET_FOR_PRIVATE_ENDPOINT = "subnetForPrivateEndpoint";

    private static final String EXISTING_PRIVATE_DNS_ZONE_ID = "existingDatabasePrivateDnsZoneId";

    private static final String SUBNETS = "subnets";

    @Inject
    private ServiceEndpointCreationToEndpointTypeConverter serviceEndpointCreationToEndpointTypeConverter;

    @Inject
    private SubnetListerService subnetListerService;

    @Inject
    private SubnetChooserService subnetChooserService;

    public Map<String, Object> addSubnetIds(List<String> subnetIds, List<String> azs, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS, GCP, MOCK -> {
                return Map.of(SUBNET_ID, String.join(",", subnetIds),
                        AVAILABILITY_ZONE, String.join(",", azs));
            }
            case AZURE -> {
                return Map.of(SUBNETS, String.join(",", subnetIds));
            }
            default -> throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
    }

    public Map<String, Object> addParameters(DetailedEnvironmentResponse environmentResponse, DBStack dbStack) {
        EnvironmentNetworkResponse network = environmentResponse.getNetwork();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(dbStack.getCloudPlatform());
        switch (cloudPlatform) {
            case AWS -> {
                return Map.of(VPC_CIDR, network.getNetworkCidr(),
                        VPC_CIDRS, network.getNetworkCidrs(),
                        VPC_ID, network.getAws().getVpcId());
            }
            case AZURE -> {
                return provideAzureParameters(environmentResponse, dbStack, network, cloudPlatform);
            }
            case GCP -> {
                if (!Strings.isNullOrEmpty(network.getGcp().getSharedProjectId())) {
                    return Map.of(SHARED_PROJECT_ID, network.getGcp().getSharedProjectId());
                } else {
                    return Map.of();
                }
            }
            case MOCK -> {
                return Map.of(VPC_ID, network.getMock().getVpcId());
            }
            default -> throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
    }

    private Map<String, Object> provideAzureParameters(DetailedEnvironmentResponse environmentResponse, DBStack dbStack, EnvironmentNetworkResponse network,
            CloudPlatform cloudPlatform) {
        Map<String, Object> parameters = new HashMap<>();
        PrivateEndpointType privateEndpointType
                = serviceEndpointCreationToEndpointTypeConverter.convert(
                network.getServiceEndpointCreation(), cloudPlatform.name());
        parameters.put(ENDPOINT_TYPE, privateEndpointType);
        Optional<String> databasePrivateDnsZoneId = Optional.ofNullable(network.getAzure())
                .map(EnvironmentNetworkAzureParams::getDatabasePrivateDnsZoneId);
        if (PrivateEndpointType.USE_PRIVATE_ENDPOINT == privateEndpointType) {
            parameters.put(SUBNET_FOR_PRIVATE_ENDPOINT, getAzureSubnetToUseWithPrivateEndpoint(environmentResponse, dbStack));
            databasePrivateDnsZoneId.ifPresent(dnsZoneId -> parameters.put(EXISTING_PRIVATE_DNS_ZONE_ID, dnsZoneId));
        }

        Optional<String> delegatedSubnet = getFlexibleServerSubnetIdFromRequestOrEnvironment(network, environmentResponse, dbStack);
        delegatedSubnet.ifPresent(subnetId -> parameters.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, subnetId));

        databasePrivateDnsZoneId.ifPresent(dnsZoneId -> parameters.put(EXISTING_PRIVATE_DNS_ZONE_ID, dnsZoneId));
        return parameters;
    }

    private Optional<String> getFlexibleServerSubnetIdFromRequestOrEnvironment(EnvironmentNetworkResponse network,
            DetailedEnvironmentResponse environmentResponse, DBStack dbStack) {
        Optional<String> optionalSubnetId = Optional.ofNullable(dbStack.getParameters())
                .map(params -> params.get(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID));
        optionalSubnetId.ifPresent(subnetId -> LOGGER.debug("Delegated subnet id from database request: {}", subnetId));
        String subscriptionId = subnetListerService.getAzureSubscriptionId(environmentResponse.getCrn());
        if (optionalSubnetId.isPresent() && isSubnetIdInEnvironmentNetworkParams(network, optionalSubnetId.get())) {
            return optionalSubnetId.map(subnetId -> subnetListerService.expandAzureResourceId(subnetId, environmentResponse, subscriptionId));
        } else {
            Optional<Set<String>> delegatedSubnetIds = Optional.ofNullable(network.getAzure())
                    .map(EnvironmentNetworkAzureParams::getFlexibleServerSubnetIds);
            Set<CloudSubnet> delegatedSubnets = delegatedSubnetIds.map(dsIds -> subnetListerService.fetchNetworksFiltered(dbStack, dsIds)).orElse(Set.of());
            LOGGER.info("Fetched delegated subnets: {}", delegatedSubnets);
            return delegatedSubnets.stream()
                    .filter(subnet -> StringUtils.isNotBlank(subnet.getCidr()))
                    .max(Comparator.comparingLong(subnet -> new SubnetUtils(subnet.getCidr()).getInfo().getAddressCountLong()))
                    .map(subnet -> subnetListerService.expandAzureResourceId(subnet, environmentResponse, subscriptionId).getId());
        }
    }

    private static Boolean isSubnetIdInEnvironmentNetworkParams(EnvironmentNetworkResponse network, String optionalSubnetId) {
        return Optional.ofNullable(network.getAzure())
                .map(EnvironmentNetworkAzureParams::getFlexibleServerSubnetIds)
                .map(subnetSet -> subnetSet.contains(optionalSubnetId))
                .orElse(false);
    }

    private String getAzureSubnetToUseWithPrivateEndpoint(DetailedEnvironmentResponse environmentResponse, DBStack dbStack) {
        String subscriptionId = subnetListerService.getAzureSubscriptionId(environmentResponse.getCrn());
        return environmentResponse.getNetwork().getSubnetMetas().values()
                .stream()
                .findFirst()
                .map(csn -> subnetListerService.expandAzureResourceId(csn, environmentResponse, subscriptionId))
                .map(CloudSubnet::getId).orElseThrow(() -> new RedbeamsException("It is not possible to create private endpoints for database: " +
                        "there are no subnets in the environment."));
    }
}
