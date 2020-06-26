package com.sequenceiq.it.cloudbreak.assertion.environment;

import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class EnvironmentNetworkTestAssertion {

    private EnvironmentNetworkTestAssertion() {
    }

    public static Assertion<EnvironmentTestDto, EnvironmentClient> environmentContainsNeccessaryConfigs() {
        return (testContext, testDto, environmentClient) -> {
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint().getByName(testDto.getName());
            if (CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
                containsRightNumberOfSubnetsOnAws(environment);
            } else if (CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
                containsRightNumberOfSubnetsOnAzure(environment);
            }
            return testDto;
        };
    }

    private static void containsRightNumberOfSubnetsOnAws(DetailedEnvironmentResponse environment) {
        if (!environment.getNetwork().getNetworkCidr().equals("10.0.0.0/16")) {
            throw new IllegalArgumentException("The network CIDR should be the same as the the request '10.0.0.0/16'!");
        }
        if (!environment.getNetwork().getPrivateSubnetCreation().equals(PrivateSubnetCreation.ENABLED)) {
            throw new IllegalArgumentException("Private subnet creation must be enabled!");
        }
        if (environment.getNetwork().getSubnetIds().size() != 6) {
            throw new IllegalArgumentException("Subnets count must be 6!");
        }
        if (environment.getNetwork().getCbSubnets().size() != 6) {
            throw new IllegalArgumentException("Cb Subnets count must be 6!");
        }
        if (environment.getNetwork().getDwxSubnets().size() != 6) {
            throw new IllegalArgumentException("Dwx Subnets count must be 6!");
        }
        if (environment.getNetwork().getMlxSubnets().size() != 6) {
            throw new IllegalArgumentException("Mlx Subnets count must be 6!");
        }
        if (environment.getNetwork().getLiftieSubnets().size() != 6) {
            throw new IllegalArgumentException("Liftie Subnets count must be 6!");
        }
        if (getSubnetsCountByType(environment, SubnetType.PUBLIC) != 3) {
            throw new IllegalArgumentException("Public Subnets count must be 3!");
        }
        if (getSubnetsCountByType(environment, SubnetType.PRIVATE) != 3) {
            throw new IllegalArgumentException("Private Subnets count must be 3!");
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
}
