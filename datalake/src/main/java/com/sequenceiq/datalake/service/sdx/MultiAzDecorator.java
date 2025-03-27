package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class MultiAzDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzDecorator.class);

    private static final Set<Tunnel> PUBLIC_SUBNET_SUPPORTED_TUNNEL = Set.of(Tunnel.DIRECT, Tunnel.CLUSTER_PROXY);

    public void decorateStackRequestWithAwsNative(StackV4Request stackV4Request) {
        stackV4Request.setVariant("AWS_NATIVE");
    }

    public void decorateStackRequestWithMultiAz(StackV4Request stackV4Request, DetailedEnvironmentResponse environment, SdxClusterShape clusterShape) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        switch (cloudPlatform) {
            case AWS -> decorateStackRequestWithMultiAzAws(stackV4Request, environment, clusterShape);
            case AZURE -> decorateStackRequestWithMultiAzAzure(stackV4Request, clusterShape);
            case GCP -> decorateStackRequestWithMultiAzGcp(stackV4Request, clusterShape);
            default -> throw new IllegalStateException("Encountered enableMultiAz==true for unsupported cloud platform " + cloudPlatform);
        }
    }

    private void decorateStackRequestWithMultiAzAws(StackV4Request stackV4Request, DetailedEnvironmentResponse environment, SdxClusterShape clusterShape) {
        stackV4Request.getInstanceGroups().forEach(ig -> {
            if (ig.getNetwork() == null) {
                ig.setNetwork(new InstanceGroupNetworkV4Request());
                InstanceGroupAwsNetworkV4Parameters networkParameter = ig.getNetwork().createAws();
                List<String> subnetIds;
                if (clusterShape.isMultiAzEnabledByDefault()) {
                    subnetIds = collectMultiAzSubnetIdsForGroup(environment, ig);
                } else {
                    subnetIds = List.of(environment.getNetwork().getPreferedSubnetId());
                }
                networkParameter.setSubnetIds(subnetIds);
            }
        });
        stackV4Request.setEnableMultiAz(true);
    }

    private void decorateStackRequestWithMultiAzAzure(StackV4Request stackV4Request, SdxClusterShape clusterShape) {
        if (!clusterShape.isMultiAzEnabledByDefault()) {
            throw new IllegalStateException(
                    String.format("Multi Az on Azure is not unsupported for Cluster Shape '%s'",
                            clusterShape));
        }
        stackV4Request.setEnableMultiAz(true);
    }

    private void decorateStackRequestWithMultiAzGcp(StackV4Request stackV4Request, SdxClusterShape clusterShape) {
        if (!clusterShape.isMultiAzEnabledByDefault()) {
            throw new IllegalStateException(
                    String.format("Multi Az on GCP is not unsupported for Cluster Shape '%s'", clusterShape));
        }
        stackV4Request.setEnableMultiAz(true);
    }

    private List<String> collectMultiAzSubnetIdsForGroup(DetailedEnvironmentResponse environment, InstanceGroupV4Request ig) {
        List<String> subnetIds;
        Map<String, CloudSubnet> cloudSubnetsBySubnetId = selectSubnetsForGroup(environment);
        if (ig.getType() == InstanceGroupType.GATEWAY) {
            subnetIds = distinctSubnetsByAz(cloudSubnetsBySubnetId);
        } else {
            subnetIds = new ArrayList<>(cloudSubnetsBySubnetId.keySet());
        }
        return subnetIds;
    }

    private static Map<String, CloudSubnet> selectSubnetsForGroup(DetailedEnvironmentResponse environment) {
        Map<String, CloudSubnet> cloudSubnetsBySubnetId = getCloudSubnetsFromCbSubnetsBasedOnDeploymentRestriction(environment);
        if (cloudSubnetsBySubnetId.isEmpty()) {
            Tunnel tunnel = environment.getTunnel();
            SubnetType subnetType;
            if (PUBLIC_SUBNET_SUPPORTED_TUNNEL.contains(tunnel)) {
                subnetType = SubnetType.PUBLIC;
                LOGGER.info("Trying to get subnets with type '{}' for Datalake's groups based on tunnel type: '{}'", subnetType, tunnel);
                cloudSubnetsBySubnetId.putAll(filterSubnetMetasBySubnetType(environment, subnetType));
            }
            if (cloudSubnetsBySubnetId.isEmpty()) {
                subnetType = SubnetType.PRIVATE;
                LOGGER.info("Trying to get subnets with type '{}' for Datalake's groups based on tunnel type: '{}'", subnetType, tunnel);
                cloudSubnetsBySubnetId.putAll(filterSubnetMetasBySubnetType(environment, subnetType));
            }
        }
        LOGGER.info("Gathered subnet ids from environment: '{}'", String.join(",", cloudSubnetsBySubnetId.keySet()));
        return cloudSubnetsBySubnetId;
    }

    private static Map<String, CloudSubnet> getCloudSubnetsFromCbSubnetsBasedOnDeploymentRestriction(DetailedEnvironmentResponse environment) {
        DeploymentRestriction datalakeDeploymentRestriction = DeploymentRestriction.DATALAKE;
        LOGGER.info("Trying to get subnets from CB set of Environment response with deployment restriction: '{}'", datalakeDeploymentRestriction);
        return Optional.ofNullable(environment.getNetwork())
                .map(EnvironmentNetworkResponse::getCbSubnets)
                .map(Map::entrySet)
                .map(cbSubnetEntrySet -> cbSubnetEntrySet.stream()
                        .filter(subnetEntry -> subnetEntry.getValue().getDeploymentRestrictions().contains(datalakeDeploymentRestriction))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
                .orElse(new HashMap<>());
    }

    private static Map<String, CloudSubnet> filterSubnetMetasBySubnetType(DetailedEnvironmentResponse environment, SubnetType subnetType) {
        return environment.getNetwork().getSubnetMetas().entrySet().stream()
                .filter(subnetEntry -> subnetType.equals(subnetEntry.getValue().getType()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private List<String> distinctSubnetsByAz(Map<String, CloudSubnet> subnetMetas) {
        Map<String, String> distinctedSubnetByAz = new HashMap<>();
        subnetMetas.forEach((subnet, cloudSubnet) -> {
            if (!distinctedSubnetByAz.containsKey(cloudSubnet.getAvailabilityZone())) {
                distinctedSubnetByAz.put(cloudSubnet.getAvailabilityZone(), subnet);
            }
        });
        return new ArrayList<>(distinctedSubnetByAz.values());
    }

    public void decorateStackRequestWithPreviousNetwork(StackV4Request stackV4Request, DetailedEnvironmentResponse environment, SdxClusterShape clusterShape,
            Map<String, Set<String>> subnetsByAz) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (cloudPlatform == CloudPlatform.AWS) {
            decorateStackRequestWithPreviousMultiAzNetworkAws(stackV4Request, subnetsByAz);
        }
    }

    private void decorateStackRequestWithPreviousMultiAzNetworkAws(StackV4Request stackV4Request, Map<String, Set<String>> subnetsByAz) {
        LOGGER.info("Decorate stackRequest with previous network configuration");

        stackV4Request.getInstanceGroups().forEach(ig -> {
            if (ig.getNetwork() == null) {
                ig.setNetwork(new InstanceGroupNetworkV4Request());
                InstanceGroupAwsNetworkV4Parameters networkParameter = ig.getNetwork().createAws();
                List<String> subnets = collectSubnetIdsFromPreviousNetwork(ig, subnetsByAz);
                LOGGER.info("Subnets used in instance group {}: {}", ig.getName(), subnets);

                networkParameter.setSubnetIds(subnets);
            }
        });
        stackV4Request.setEnableMultiAz(true);
    }

    private List<String> collectSubnetIdsFromPreviousNetwork(InstanceGroupV4Request ig, Map<String, Set<String>> subnetsByAz) {
        if (ig.getType() == InstanceGroupType.GATEWAY) {
            return getOneSubnetFromEachAvailabilityZone(subnetsByAz);
        } else {
            return subnetsByAz
                    .values()
                    .stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());
        }
    }

    private List<String> getOneSubnetFromEachAvailabilityZone(Map<String, Set<String>> subnetsByAz) {
        return subnetsByAz
                .values()
                .stream()
                .map(subnetsSet -> subnetsSet.stream()
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}