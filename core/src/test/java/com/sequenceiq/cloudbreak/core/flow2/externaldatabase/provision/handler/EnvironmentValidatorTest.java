package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

class EnvironmentValidatorTest {

    private static final String STACKNAME = "stackname";

    private final EnvironmentValidator underTest = new EnvironmentValidator();

    @BeforeEach
    void setUp() {
    }

    @Test
    void checkValidEnvironmentAzure() {
        DetailedEnvironmentResponse environment = createEnvironment(CloudPlatform.AZURE.name(), Map.of());
        underTest.checkValidEnvironment(STACKNAME, DatabaseAvailabilityType.HA, environment);
    }

    @Test
    void checkValidEnvironmentAwsNonHA() {
        DetailedEnvironmentResponse environment = createEnvironment(CloudPlatform.AWS.name(), Map.of());
        underTest.checkValidEnvironment(STACKNAME, DatabaseAvailabilityType.NON_HA, environment);
    }

    @Test
    void checkValidEnvironmentAwsHANotEnoughSubnets() {
        Map<String, CloudSubnet> subnetMetas = Map.of("subnet1", new CloudSubnet());
        DetailedEnvironmentResponse environment = createEnvironment(CloudPlatform.AWS.name(), subnetMetas);
        assertThatThrownBy(() -> underTest.checkValidEnvironment(STACKNAME, DatabaseAvailabilityType.HA, environment))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("not enough subnets in the vpc");
    }

    @Test
    void checkValidEnvironmentAwsHANotEnoughAZs() {
        Map<String, CloudSubnet> subnetMetas = Map.of("subnet1", new CloudSubnet("id1", "name1", "az1", "cidr"),
                "subnet2", new CloudSubnet("id2", "name2", "az1", "cidr"));

        DetailedEnvironmentResponse environment = createEnvironment(CloudPlatform.AWS.name(), subnetMetas);
        assertThatThrownBy(() -> underTest.checkValidEnvironment(STACKNAME, DatabaseAvailabilityType.HA, environment))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vpc subnets must cover at least two different availability zones");
    }

    @Test
    void checkValidEnvironmentAwsHA() {
        Map<String, CloudSubnet> subnetMetas = Map.of("subnet1", new CloudSubnet("id1", "name1", "az1", "cidr"),
                "subnet2", new CloudSubnet("id2", "name2", "az2", "cidr"));

        DetailedEnvironmentResponse environment = createEnvironment(CloudPlatform.AWS.name(), subnetMetas);
        underTest.checkValidEnvironment(STACKNAME, DatabaseAvailabilityType.HA, environment);
    }

    private DetailedEnvironmentResponse createEnvironment(String cloudplatform, Map<String, CloudSubnet> subnetMetas) {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setCloudPlatform(cloudplatform);
        EnvironmentNetworkResponse network = createNetwork(subnetMetas);
        env.setNetwork(network);
        return env;
    }

    private EnvironmentNetworkResponse createNetwork(Map<String, CloudSubnet> subnetMetas) {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(subnetMetas);
        return network;
    }
}
