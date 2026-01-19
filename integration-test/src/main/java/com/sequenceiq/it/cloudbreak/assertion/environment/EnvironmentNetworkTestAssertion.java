package com.sequenceiq.it.cloudbreak.assertion.environment;

import static com.sequenceiq.common.api.type.DeploymentRestriction.ENDPOINT_ACCESS_GATEWAY;

import java.util.Collection;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentNetworkTestAssertion {

    private EnvironmentNetworkTestAssertion() {
    }

    public static Assertion<EnvironmentTestDto, EnvironmentClient> environmentContainsNeccessaryConfigs() {
        return (testContext, testDto, environmentClient) -> {
            DetailedEnvironmentResponse environment = environmentClient.getDefaultClient(testContext).environmentV1Endpoint().getByName(testDto.getName());
            if (CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
                containsRightNumberOfSubnetsOnAws(environment);
                hasThreePrivateSubnet(environment);
            } else if (CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
                containsRightNumberOfSubnetsOnAzure(environment);
            }
            return testDto;
        };
    }

    private static void hasThreePrivateSubnet(DetailedEnvironmentResponse environment) {
        long numberOfPrivateSubnet = environment.getNetwork().getCbSubnets().values().stream()
                .filter(cloudSubnet -> cloudSubnet.getType().equals(SubnetType.PRIVATE))
                .count();
        if (numberOfPrivateSubnet != 3) {
            throw new IllegalArgumentException("the number of private networks should be 3");
        }
    }

    private static void containsRightNumberOfSubnetsOnAws(DetailedEnvironmentResponse environment) {
        if (!environment.getNetwork().getNetworkCidr().equals("10.0.0.0/16")) {
            throw new IllegalArgumentException("The network CIDR should be the same as the the request '10.0.0.0/16'!");
        }
        if (!environment.getNetwork().getPrivateSubnetCreation().equals(PrivateSubnetCreation.ENABLED)) {
            throw new IllegalArgumentException("Private subnet creation must be enabled!");
        }
        if (environment.getNetwork().getSubnetIds().size() != 6) {
            throw new IllegalArgumentException("Subnets count must be 6! Created subnets: " + environment.getNetwork().getSubnetIds().size());
        }
        if (environment.getNetwork().getCbSubnets().size() != 6) {
            throw new IllegalArgumentException("Cb Subnets count must be 6! Created subnets: " + environment.getNetwork().getCbSubnets().size());
        }
        if (environment.getNetwork().getDwxSubnets().size() != 6) {
            throw new IllegalArgumentException("Dwx Subnets count must be 6! Created subnets: " + environment.getNetwork().getDwxSubnets().size());
        }
        if (environment.getNetwork().getMlxSubnets().size() != 6) {
            throw new IllegalArgumentException("Mlx Subnets count must be 6! Created subnets: " + environment.getNetwork().getMlxSubnets().size());
        }
        if (environment.getNetwork().getLiftieSubnets().size() != 6) {
            throw new IllegalArgumentException("Liftie Subnets count must be 6! Created subnets: " + environment.getNetwork().getLiftieSubnets().size());
        }
        if (getSubnetsCountByType(environment, SubnetType.PUBLIC) != 3) {
            throw new IllegalArgumentException("Public Subnets count must be 3! Created subnets: " + getSubnetsCountByType(environment, SubnetType.PUBLIC));
        }
        if (getSubnetsCountByType(environment, SubnetType.PRIVATE) != 3) {
            throw new IllegalArgumentException("Private Subnets count must be 3! Created subnets: " + getSubnetsCountByType(environment, SubnetType.PRIVATE));
        }
    }

    private static void containsRightNumberOfSubnetsOnAzure(DetailedEnvironmentResponse environment) {
        if (!environment.getNetwork().getNetworkCidr().equals("10.0.0.0/16")) {
            throw new IllegalArgumentException("The network CIDR should be the same as the the request '10.0.0.0/16'!");
        }
        if (!environment.getNetwork().getPrivateSubnetCreation().equals(PrivateSubnetCreation.ENABLED)) {
            throw new IllegalArgumentException("Private subnet creation must be enabled!");
        }
        if (environment.getNetwork().getCbSubnets().size() != 6) {
            throw new IllegalArgumentException("Cb Subnets count must be 6!");
        }
        if (environment.getNetwork().getDwxSubnets().size() != 3) {
            throw new IllegalArgumentException("Dwx Subnets count must be 3!");
        }
        if (environment.getNetwork().getMlxSubnets().size() != 32) {
            throw new IllegalArgumentException("Mlx Subnets count must be 32!");
        }
        if (environment.getNetwork().getLiftieSubnets().size() != 32) {
            throw new IllegalArgumentException("Liftie Subnets count must be 32!");
        }
        if (environment.getNetwork().getSubnetMetas().size() != 41) {
            throw new IllegalArgumentException("Identities should not be null in response and should be 41!");
        }
        if (getSubnetsCountByType(environment, SubnetType.PUBLIC) != 3) {
            throw new IllegalArgumentException("Public Subnets count must be 3!");
        }
        if (getSubnetsCountByType(environment, SubnetType.PRIVATE) != 3) {
            throw new IllegalArgumentException("Private Subnets count must be 3!");
        }
    }

    private static int getSubnetsCountByType(DetailedEnvironmentResponse environment, SubnetType type) {
        return environment.getNetwork().getSubnetMetas()
                .values()
                .stream()
                .filter(e -> e.getType().equals(type))
                .collect(Collectors.toList())
                .size();
    }

    public static Assertion<EnvironmentTestDto, EnvironmentClient> environmentWithEndpointGatewayContainsNeccessaryConfigs(Collection<String> workloadSubnetIds,
            Collection<String> loadBalancerSubnetIds) {
        return (testContext, testDto, environmentClient) -> {
            DetailedEnvironmentResponse environment = environmentClient.getDefaultClient(testContext).environmentV1Endpoint().getByName(testDto.getName());
            isPublicEndpointAccessGatewayEnabled(environment);
            loadBalancerSubnetIdsWerePropagatedCorrectly(environment, loadBalancerSubnetIds);
            workloadSubnetIdsWerePropagatedCorrectly(environment, workloadSubnetIds);
            return testDto;
        };
    }

    private static void isPublicEndpointAccessGatewayEnabled(DetailedEnvironmentResponse environment) {
        if (environment.getNetwork() == null ||
                environment.getNetwork().getPublicEndpointAccessGateway() != PublicEndpointAccessGateway.ENABLED) {
            throw new IllegalArgumentException("Public endpoint access gateway should be enabled!");
        }
    }

    private static void loadBalancerSubnetIdsWerePropagatedCorrectly(DetailedEnvironmentResponse environment, Collection<String> expectedSubnetIds) {
        if (!expectedSubnetIds.equals(environment.getNetwork().getEndpointGatewaySubnetIds())) {
            throw new IllegalArgumentException(String.format("Public endpoint access gateway subnet ids should be set to %s!", expectedSubnetIds));
        }
        for (CloudSubnet cloudSubnet : environment.getNetwork().getSubnetMetas().values()) {
            if (expectedSubnetIds.contains(cloudSubnet.getId()) && !cloudSubnet.getDeploymentRestrictions().contains(ENDPOINT_ACCESS_GATEWAY)) {
                throw new IllegalStateException(String.format("The %s deployment restriction should not miss from the list!", ENDPOINT_ACCESS_GATEWAY.name()));
            }
        }
    }

    private static void workloadSubnetIdsWerePropagatedCorrectly(DetailedEnvironmentResponse environment, Collection<String> expectedSubnetIds) {
        if (!expectedSubnetIds.equals(environment.getNetwork().getSubnetIds())) {
            throw new IllegalArgumentException(String.format("The subnet IDs should be set to %s!", expectedSubnetIds));
        }
        for (CloudSubnet cloudSubnet : environment.getNetwork().getSubnetMetas().values()) {
            if (expectedSubnetIds.contains(cloudSubnet.getId()) && cloudSubnet.getDeploymentRestrictions().contains(ENDPOINT_ACCESS_GATEWAY)) {
                throw new IllegalStateException(String.format("The %s deployment restriction should miss from the list restriction list!",
                        ENDPOINT_ACCESS_GATEWAY.name()));
            }
        }
    }

}
